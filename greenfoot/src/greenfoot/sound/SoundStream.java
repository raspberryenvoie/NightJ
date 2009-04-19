/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.sound;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;


import bluej.Config;
import bluej.utility.Debug;

/**
 * Plays sound from a URL. To avoid loading the entire sound clip into memory,
 * the sound is streamed.
 * <p>
 * There are several inconsistencies between different platforms that means that
 * this class is more complicated than it really should be if everything worked
 * as it should. Below is listed the different problems observed on various
 * platforms:
 * <p>
 * Windows XP on Poul's home PC (SP3, Sun JDK 1.6.11, SB Live Sound Card) and
 * Windows Vista on Poul's office PC (dell build in soundcard) and Windows XP on
 * Poul's office PC (SP2, dell build in soundcard)
 * <ul>
 * <li>Line does not receive a stop signal when end of media has been reached.</li>
 * <li>Line is reported as active even when end of media has been reached. If
 * invoking stop, then start again, it seems to remain inactive though (this
 * does not generate a START event, only a stop)</li>
 * <li>The frame position reported by line.getLongFramePosition() is incorrect.
 * After reaching the last frame, it will, after a while, start over at frame
 * position 0 and count up to the last frame again.</li>
 * </ul>
 * <p>
 * Linux on Poul's home PC (Ubuntu 8.10, Sun JDK 1.6.10, SB Live Sound Card):
 * <ul>
 * <li>Line does not receive a stop signal when end of media has been reached.</li>
 * <li>Line is reported as active even when end of media has been reached.</li>
 * <li>Hangs if line.drain() is used (need to confirm this, saw it a long time
 * ago, and it might have been because of timing issues resulting in drain()
 * being invoked on a stopped line)</li>
 * <li>The frame position reported by line.getLongFramePosition() is correct and
 * seems to be the only way of detecting when the end of the media has been
 * reached.</li>
 * </ul>
 * <p>
 * <p>
 * Linux on Poul's office PC (Ubuntu 8.10, Sun JDK 1.6.10 / 1.5.16, SB Live
 * Sound Card):
 * <ul>
 * <li>Seems to work without any problems. It gets the stop event correctly, it
 * goes from active to inactive when reaching the end.</li>
 * <li>Haven't tested whether line.drain() works though.</li>
 * 
 * </ul>
 * <p>
 * Mac (OS 10.5.6, JDK 1.5.0_16
 * <ul>
 * <li>Closing and opening a line repeatedly crashes the JVM with this error
 * (JDK 1.5): <br>
 * java(3382,0xb1b4e000) malloc: *** mmap(size=1073745920) failed (error
 * code=12)<br>
 * error: can't allocate region<br>
 * set a breakpoint in malloc_error_break to debug</li>
 * <li>It skips START events if the line is closed before we have received the
 * START event.</li>
 * </ul>
 * 
 * 
 * <p>
 * So, on Linux we can only use the frame position as indicator of when the
 * playback has finished, which will only work correctly if we use line.close()
 * and line.open() to reset the frame position to 0 every time playback is
 * started or restarted. To avoid using close()/open() I tried marking the frame
 * position at which playback was restarted to offset the frame position, but
 * this is not reliable on mac at least, so I don't trust it for other systems
 * either.
 * <p>
 * On Mac we cannot use line.close()/line.open() at all because it crashes the
 * JVM badly. This also means that we cannot use line.drain() because the only
 * way of interrupting that is to call close() on the line. We do get the
 * correct START events if we don't close the line prematurely though, so this,
 * in conjunction with the frame position can be used to determine end of media.
 * <p>
 * On windows, we could use drain() and close() to make it work. We have to make
 * sure that the line is not stopped before invoking drain though, which could
 * be difficult. Probably need a flag to indicate whether a stop request has
 * been send. Probably better to make it more similar to Linux though to avoid
 * too many different implementations.
 * 
 * <p>
 * 
 * For windows and Linux I can probably use the same implementation, by using
 * close/open and frame position. For Windows I have to be aware that it might
 * reset the frame position though, and watch for a decrease in frame position
 * to detect end of the stream in case it misses the end frame.
 * 
 * On mac I have to avoid using open/close.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundStream extends Sound implements Runnable
{
    private static final boolean DEBUG = true;
    private static void printDebug(String s) 
    {
        if(DEBUG) {
            System.out.println(s);
        }
    }

    /**
	 * Flag that indicates whether it is safe to use open and close on the
	 * SourceDataLine.
	 */
	private boolean useCloseAndOpen;
    
    /**
     * How long to wait until closing the line and stopping the playback thread
     * after playback has finished. In ms.
     */
    private static final int CLOSE_TIMEOUT = 1000;
    
    /**
     * URL of the stream of sound data.
     */
    private URL url;
    
    /**
     * Flag that indicates that the playback should stop.
     */
    private boolean stop;
    
    /**
     * Flag that indicates that the playback should pause.
     */
    private boolean pause; 
    
    /** Flag that indicates whether the sound is currently playing. (it can be paused)*/
    private volatile boolean playing = false;
    
    /** Flag that indicates that playback should start over from the beginning */
    private boolean restart;

    /** Listener for state changes. */
    private SoundPlaybackListener playbackListener;
    
    /** The line that we play the sound through */
    private SourceDataLine line;
    
    /** Therad that handles the actual playback of the sound. */
    private Thread playThread ;

    /** Used to keep track of when a line has engaged in playback */ 
    private boolean gotStartEvent = false;
   
    /**
	 * Used to detect whether the frame position of the line decreases when it
	 * shouldn't. This can happen on windows after playback has finished: it
	 * will reset the frame position to 0 and go back up to the end frame.
	 */
	private long previousFramePosition;
    
    public SoundStream(URL url, SoundPlaybackListener playbackListener)
        throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        this.url = url;
        stop = false;
        this.playbackListener = playbackListener;
        if(Config.isMacOS()) {
        	useCloseAndOpen = false;
        } else {
        	useCloseAndOpen = true;
        }
    }


    public synchronized void stop()
    {
        if (!stop) {
            stop = true;
            notifyAll();
            playbackListener.playbackStopped(this);
        }
    }
    public synchronized void pause()
    {
        if (!pause) {
            pause = true;
            notifyAll();
            playbackListener.playbackPaused(this);
        }
    }

    public synchronized void resume()
    {
        if (pause) {
            printDebug("resume() called");
            pause = false;
            notifyAll();
            playbackListener.playbackStarted(this);
        }
    }
    
    public boolean isPlaying() 
    {
        return playing;
    }

    public synchronized void play()
    {
        restart = true;
        pause = false;
        stop = false;
        if (playThread == null) {
            printDebug("Starting new playthread");
            playThread = new Thread(this, "SoundStream:" + url.toString());
            playThread.start();
        }
        notifyAll();
        playbackListener.playbackStarted(this);
    }

    public String toString()
    {
        return url + " " + super.toString();
    }
    

    public void run()
    {
        boolean stayAlive = true; // Whether the thread should stay alive or die.

        AudioInputStream inputStream = null;
        try {
            while (stayAlive) {

                if (inputStream != null) {
                    inputStream.close();
                }
                inputStream = AudioSystem.getAudioInputStream(url);
                AudioFormat format = inputStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                
                
                synchronized (this) {
                    if (line == null) {
                        line = (SourceDataLine) AudioSystem.getLine(info);
                        printDebug("buffer size: " + line.getBufferSize());
                        line.addLineListener(new LineListener(){
                            public void update(LineEvent event)
                            {
                                printDebug("Got event: " + event);
                                if(event.getType() == LineEvent.Type.START || event.getType() == LineEvent.Type.CLOSE) {
                                    synchronized(this){
                                        gotStartEvent = true;
                                    }
                                }
                                printDebug("Got event END : " + event);
                            }});
                    }
                    line.open(format);
                    
                    
                }

                int frameSize = format.getFrameSize();

                printDebug("Stream available: " + inputStream.available() + " in frames: " + inputStream.available()
                        / frameSize);
                
                byte[] buffer = new byte[getBufferSizeToHold500ms(format)];

                int bytesInBuffer = 0;
                long totalFramesWritten = 0;
                previousFramePosition = 0;
                
                // The frame at which playback was started. Used to determine if
                // playback has begun at all.
                long startFrame = line.getLongFramePosition();
                synchronized (this) {
                    restart = false;
                }
                int bytesRead = inputStream.read(buffer, 0, buffer.length);
                bytesInBuffer = bytesRead;
                printDebug(" read: " + bytesRead);
                while (bytesInBuffer > 0) {

                    int written = 0;
                    // Only write in multiples of frameSize
                    int bytesToWrite = (bytesInBuffer / frameSize) * frameSize;
                    synchronized (this) {
                    	if(useCloseAndOpen && !line.isOpen()) {
                    		line.open(format);
                    	}
                        line.start();
                        while (line.available() < line.getBufferSize()/4 ) {
                            // Not much space available in buffer right now, lets wait
                            // a bit until we can write a good chunk.
                            if (restart || stop || pause) {
                                printDebug("restart: " + restart + " pause: " + pause);
                                break;
                            }
                            try {
                            	int timeLeft = getTimeToPlayBytes(line.getBufferSize() - line.available(), format);
                            	printDebug(" time left : " + timeLeft);
                            	if (timeLeft > 16) {
									// There is still quite a bit of time left
									// before the data becomes available. We
									// wait a bit.
                            		wait(timeLeft/4);
                            	}
                            	else if( timeLeft < 0) {
                            		// Could not figure out how much time was
									// left.
									// Waiting 20ms should be quick enough to
									// still be able to supply the line with
									// data at a fast enough rate
                            		wait(20);
                            	}
                            }
                            catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }

                        // Handle stop
                        if (stop)
                            break;

                        // Handle pause
                        while (pause) {
                            try {
                                printDebug("In pause loop");
                                playing = false;
                                line.stop();
                                gotStartEvent=false;
                                printDebug("In pause loop 2");
                                this.wait();
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        
                        // In case it was paused, restart it.
                        line.start(); 
                        playing = true;

                        // Handle restart
                        if (restart) {
                            printDebug("restart in thread");
                            line.stop();
                            line.flush();
                            line.close();
                            gotStartEvent=false;
                            try {
                                inputStream.close();
                            }
                            catch (IOException e) {}
                            inputStream = AudioSystem.getAudioInputStream(url);
                            restart = false;

                            totalFramesWritten = 0;
                            bytesInBuffer = 0;
                            bytesRead = 0;
                            bytesToWrite = 0;
                            previousFramePosition = 0;
                            printDebug("inputStream available after restart in thread: " + inputStream.available());

                        }

                        // Only write what can be written without blocking
                        if (bytesToWrite > line.available()) {
                            bytesToWrite = line.available();
                        }

                        // Play it
                        written = line.write(buffer, 0, bytesToWrite);

                        printDebug(" wrote: " + written);
                    }

                    totalFramesWritten += written / frameSize;

                    // Copy remaining bytes (if we wrote less than what is in
                    // the buffer)
                    int remaining = bytesInBuffer - written;
                    if (remaining > 0) {
                        printDebug("remaining: " + remaining + "  written: " + written + "   bytesInBuffer: "
                                + bytesInBuffer + "   bytesToWrite: " + bytesToWrite);
                        System.arraycopy(buffer, written, buffer, 0, remaining);
                    }
                    bytesInBuffer = remaining;

                    bytesRead = inputStream.read(buffer, bytesInBuffer, buffer.length - bytesInBuffer);
                    if(bytesRead != -1) {
                        bytesInBuffer += bytesRead;
                    }
                    printDebug(" read: " + bytesRead);
                }

                // We are now done writing the data to the line, but it might
                // still be playing, so we have to wait till playback has
                // finished before closing the line.

                printDebug("Frames written: " + totalFramesWritten);
                printDebug("before waiting for playback to end on line: " + line + "  framePos: "
                        + line.getFramePosition()  + "  avail:" + line.available() + "  active:" + line.isActive()
                        + "  open:" + line.isOpen() + "  runnig:" + line.isRunning());
                synchronized (this) {
                    // While we are still actively playing things, and don't
                    // receive any signals
                    while (isLinePlaying(startFrame, totalFramesWritten) && !restart && !stop) {                        
                        printDebug("waiting " + line + "  framePos: " + line.getLongFramePosition() + "  msPos: "
                                + line.getMicrosecondPosition()+ "  avail:"
                                + line.available() + "  active:" + line.isActive() + "  open:" + line.isOpen()
                                + "  runnig:" + line.isRunning() + "  startframe: " + startFrame + "  gotStartEvent: " + gotStartEvent);
                        try {
                            if (pause) {
                                line.stop();
                                this.wait();
                                gotStartEvent=false;
                                line.start();
                            }
                            else {
                            	int bytesLeft = line.getBufferSize() - line.available();
                            	int timeLeft = getTimeToPlayBytes(bytesLeft, format);
                            	printDebug(" time left: " + timeLeft);
                            	if(timeLeft > 50) {
                            		wait(timeLeft);
                            	}
                            	else {
                            		wait(20);
                            	}
                            }
                        }
                        catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            break;
                        }
                    }
                    
                    playing = false;

                    printDebug("after  " + line + "  framePos: " + line.getFramePosition() + "  msPos: "
                            + line.getMicrosecondPosition() + "  avail:"
                            + line.available() + "  active:" + line.isActive() + "  open:" + line.isOpen()
                            + "  running:" + line.isRunning());
                    printDebug(" 1 restart =  " + restart + "  stop = " + stop);

                    

                    // NOTE: If the size of the stream is a multiple of 64k (=
                    // 16k
                    // frames)
                    // then it plays the last 64k twice if I don't stop it here.
                    // It still has a strange clicking sound at the end, which
                    // is probably because it starts playing a bit of the extra,
                    // but is stopped before it finishes.
                    // To make this more explicit, add a delay before line.stop.
                    // For example 4d.wav from piano scenario. Happens on my
                    // macbook and Ubuntu in the office. Poul.
                    line.stop();
                    line.flush(); 
                    if(useCloseAndOpen) {
                    	line.close();
                    }
                    gotStartEvent=false;
                    
                    if (!restart || stop) {
                        // Have a short pause before we get rid of the thread,
                        // in case the sound is played again soon after.
                        try {
                            printDebug("WAIT");
                            this.wait(CLOSE_TIMEOUT);
                        }
                        catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        // Kill thread if we have not received a signal to
                        // continue playback
                        if (!restart || stop) {
                            stayAlive = false;
                            playing = false;
                            playThread = null;
                            printDebug("KILL THREAD");
                        } 
                    }
                    

                    printDebug(" 2 restart =  " + restart + "  stop = " + stop);

                    // If a restart was signalled, remove the signal and
                    // just continue. 
                    if(restart) {
                        restart = false;                       
                    }
                }

            }
        }
        // TODO: only show some exceptions once, maybe create a centralised
        // sound exception handler.
        /*catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
        catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            Debug.reportError("Error when streaming sound.", e);
        }
        finally {
            synchronized (this) {
                playing = false;
                playThread = null;
                if (line != null) {
                    line.close();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            playbackListener.playbackStopped(this);
        }
    }

    /**
     * Calculate how long it will take to play the given number of bytes. 
     * @param bytes Number of bytes.
     * @param format The format used to play the bytes.
     * @return time in ms or -1 if it could not be calculated.
     */
    private int getTimeToPlayBytes(int bytes, AudioFormat format) {
    	if(format.getFrameRate() != AudioSystem.NOT_SPECIFIED) {
    		return (int) (1000 * bytes  / (format.getFrameRate() * format.getFrameSize())) ;
    	} else {
    		return -1;
    	}
	}


	/**
     * Will attempt to calculate a buffer size that can hold half a second of audio data.
     * If unsuccessful it will default to 64k buffer size.
     */
	private int getBufferSizeToHold500ms(AudioFormat format) {
		int bufferSize;
		if(format.getFrameRate() != AudioSystem.NOT_SPECIFIED){
			bufferSize = (int) Math.ceil( format.getFrameSize() * format.getFrameRate() / 2);
		} 
		else if(format.getSampleRate() != AudioSystem.NOT_SPECIFIED) {
			bufferSize = (int) Math.ceil( (format.getSampleSizeInBits() / 8) * format.getChannels() * format.getSampleRate() / 2);
		}
		else {
			bufferSize = 64 * 1024;
		}
		printDebug("readbuffer Size: " + bufferSize);
		return bufferSize;
	}


    /**
     * True if the line has not yet finished playback.
     * 
     * Should only be called when synchronized on line.
     * 
     * @param startFrame The frame at which playback was started.
     * @param framesWritten Number of frames written to the line.
     * @return True is the line is playing, or has not started playing yet.
     */
	private boolean isLinePlaying(long startFrame, long framesWritten) {
		long currentFramePosition =  line.getLongFramePosition(); 
		if(useCloseAndOpen) {
			// One of two things can indicate that we are still playing>
			// 1. If the current frame position is smaller than the frame written
			// 2. If silly Windows has not yet reset the frame index back to 0.
			boolean res = currentFramePosition < framesWritten && previousFramePosition <= currentFramePosition;
			previousFramePosition = currentFramePosition;
			return res;
		}
		else {
			// We first need to check if we haven't finished (or
			// started) playback yet, which is true if:
			//
			// * We didn't get a start event since the last time we
			// stopped (means that we haven't started playback yet)
			//
			// * If the line is inactive and we have not progressed
			// past the startFrame (means that we haven't started
			// playback yet)
			//
			// * The line is still active (means that we are currently 
			// playing)
			//
			// Although you might think the above should be related
			// somehow, it is not the case, and if we don't check each
			// one, we might end the playback to soon.
			// 
			return (!gotStartEvent || (!line.isActive() && currentFramePosition <= startFrame ) || line.isActive() );
		}
	}
}
