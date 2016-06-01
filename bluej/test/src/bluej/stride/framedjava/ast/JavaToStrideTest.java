package bluej.stride.framedjava.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.elements.BreakElement;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.CommentElement;
import bluej.stride.framedjava.elements.ConstructorElement;
import bluej.stride.framedjava.elements.IfElement;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.elements.LocatableElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.ReturnElement;
import bluej.stride.framedjava.elements.ThrowElement;
import bluej.stride.framedjava.elements.TryElement;
import bluej.stride.framedjava.elements.VarElement;
import bluej.stride.framedjava.elements.WhileElement;
import org.junit.Assert;
import org.junit.Test;

/**
 * Note: some of the test cases are not semantically valid, but as long as they are syntactically valid
 * (in Java and Stride) that is fine for parser tests.
 */
public class JavaToStrideTest
{
    @Test
    public void testStatements()
    {
        assertEquals("while (true);", _while("true"));
        assertEquals("while (true) while (false);", _while("true", _while("false")));

        assertEquals("while (true) {}", _while("true"));
        assertEquals("while (true) {while (false); while (0);}", _while("true", _while("false"), _while("0")));

        assertEquals("while (0) {while (1) { while (2) {} } while (3) while (4); while(5); }",
                _while("0", _while("1", _while("2")), _while("3", _while("4")), _while("5")));

        assertEquals("return 0;", _return("0"));
        assertEquals("return 0+1;", _return("0 + 1"));
        assertEquals("return 0+(1+2);", _return("0 + ( 1 + 2 )"));
        assertEquals("return;", _return());

        assertEquals("break;", new BreakElement(null, true));

        assertEquals("throw e;", new ThrowElement(null, filled("e"), true));
        assertEquals("throw new IOException();", new ThrowElement(null, filled("new IOException ( )"), true));

        assertEquals("try { return 0; } catch (Exception e) { return 1; }", _try(l(_return("0")), l(new TypeSlotFragment("Exception", "Exception")), l(new NameDefSlotFragment("e")), l(l(_return("1"))), null));
        assertEquals("try { return 0; } finally { return 1;}", _try(l(_return("0")), l(), l(), l(), l(_return("1"))));
        assertEquals("try { return 0; } catch (E1 e1) { return 1; } catch (E2 e2) { return 2; } finally { return -1;}", _try(l(_return("0")), l(new TypeSlotFragment("E1", "E1"), new TypeSlotFragment("E2", "E2")), l(new NameDefSlotFragment("e1"), new NameDefSlotFragment("e2")), l(l(_return("1")), l(_return("2"))), l(_return("- 1"))));
        assertEquals("try { return 0; } catch (E1A|E1B e1) { return 1; } catch (E2A|E2B|E2C e2) { return 2; } finally { return -1;}",
                _try(l(_return("0")),
                        l(new TypeSlotFragment("E1A", "E1A"), new TypeSlotFragment("E1B", "E1B"), new TypeSlotFragment("E2A", "E2A"), new TypeSlotFragment("E2B", "E2B"), new TypeSlotFragment("E2C", "E2C")),
                        l(new NameDefSlotFragment("e1"), new NameDefSlotFragment("e1"), new NameDefSlotFragment("e2"), new NameDefSlotFragment("e2"), new NameDefSlotFragment("e2")),
                        l(l(_return("1")), l(_return("1")), l(_return("2")), l(_return("2")), l(_return("2"))), l(_return("- 1"))));
    }

    private TryElement _try(List<CodeElement> tryContents, List<TypeSlotFragment> catchTypes, List<NameDefSlotFragment> catchNames, List<List<CodeElement>> catchContents, List<CodeElement> finallyContents)
    {
        return new TryElement(null, tryContents, catchTypes, catchNames, catchContents, finallyContents, true);
    }

    @Test
    public void testIf()
    {
        assertEquals("if (0);", _if("0"));
        assertEquals("if (0) return 1;", _if("0", _return("1")));
        assertEquals("if (0) return 1; else return 2;", _ifElse("0", Arrays.asList(_return("1")), Arrays.asList(_return("2"))));
        assertEquals("if (0) return 1; else if (2) return 3; else if (4) return 5; else return 6;",
            _ifElseIf("0", Arrays.asList(_return("1")),
                      Arrays.asList("2", "4"), Arrays.asList(Arrays.asList(_return("3")), Arrays.asList(_return("5"))),
                      Arrays.asList(_return("6"))));
        assertEquals("if (0) return 1; else if (2) return 3; else if (4) return 5;",
            _ifElseIf("0", Arrays.asList(_return("1")),
                Arrays.asList("2", "4"), Arrays.asList(Arrays.asList(_return("3")), Arrays.asList(_return("5"))),
                null));
    }
    
    @Test
    public void testCall()
    {
        assertEquals("go();", _call("go ( )"));
        assertEquals("move(6 + 7);", _call("move ( 6 + 7 )"));
        assertEquals("getFoo().move(6 + 7);", _call("getFoo ( ) . move ( 6 + 7 )"));
        
        // Assignments will become call-frames initially, even though on insertion
        // as real code, CallFrame will check and self-convert to AssignFrame:
        assertEquals("x = getX();", _call("x = getX ( )"));
    }
    
    @Test
    public void testExpression()
    {
        assertExpression("0", "0");
        assertExpression("0 + 1", "0+1");
        assertExpression("0 + 1", "0 + 1");
        assertExpression("0 + 1", "0  +  1");
        assertExpression("0 >= 1", "0  >=  1");
        assertExpression("0 > = 1", "0  > =  1");
        assertExpression("new Foo ( )", "new Foo()");
        assertExpression("newFoo ( )", "newFoo()");
        assertExpression("<:", "instanceof");
        assertExpression("a <: b", "a instanceof b");
        assertExpression("a instanceofb", "a instanceofb");
        // Confusingly, if you put <: in from the Java side, you should get < : (two operators):
        assertExpression("a < : b", "a <: b");
        
        //TODO test casts (seems to be a problem)
    }
    
    @Test
    public void testInstanceof()
    {
        assertEquals("while (a instanceof b) {}", _while(new FilledExpressionSlotFragment("a <: b", "a instanceof b")));
        assertEquals("if (a instanceof b) {} else if (c instanceof d) {}", _ifElseIf(new FilledExpressionSlotFragment("a <: b", "a instanceof b"), l(), l(new FilledExpressionSlotFragment("c <: d", "c instanceof d")), l(l()), null));
        assertEquals("foo(a instanceof b);", _call(new CallExpressionSlotFragment("foo ( a <: b )", "foo ( a instanceof b )")));
        assertEquals("return (a instanceof b);", _return(new OptionalExpressionSlotFragment("( a <: b )", "( a instanceof b )")));
        assertEqualsMember("C() {super(a instanceof b);}", _constructorDelegate(null, AccessPermission.PROTECTED, l(), l(), SuperThis.SUPER, new SuperThisParamsExpressionFragment("a <: b", "a instanceof b"), l()));
    }
    
    @Test
    public void testMethod()
    {
        assertEqualsMember("public void foo() { return; }", _method(null, AccessPermission.PUBLIC, false, false, "void", "foo", Collections.emptyList(), Collections.emptyList(), Arrays.asList(_return())));
        assertEqualsMember("public final void foo(int x) { return; }", _method(null, AccessPermission.PUBLIC, false, true, "void", "foo", Arrays.asList(_param("int", "x")), Collections.emptyList(), Arrays.asList(_return())));

        assertEqualsMember("Foo(int x, String y) throws IOException { }",
            _constructor(null, AccessPermission.PROTECTED, Arrays.asList(_param("int", "x"), _param("String", "y")),
                Arrays.asList("IOException"), Arrays.asList()));
     
        assertEqualsMember("/** Comment */ private void foo() {}", _method("Comment", AccessPermission.PRIVATE, false, false, "void", "foo", Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        assertEqualsMember("/** Multi\n * line \n * comment.\n*/ private static final java.lang.String foo() throws IOException, NullPointerException {}",
            _method("Multi line comment.", AccessPermission.PRIVATE, true, true, "java.lang.String", "foo",
                Collections.emptyList(), Arrays.asList("IOException", "NullPointerException"), Collections.emptyList()));
        assertEqualsMember("/** First\nPara.\n\nSecond\nPara.\n\n\nThird Para.*/\nprotected Foo(){}",
            _constructor("First Para.\nSecond Para.\n\nThird Para.",
                AccessPermission.PROTECTED, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        
        assertEqualsMember("// X\npublic Bar() { this(0); }", _constructorDelegate("X", AccessPermission.PUBLIC, Collections.emptyList(), Collections.emptyList(), SuperThis.THIS, "0", Collections.emptyList()));
        assertEqualsMember("C() {this2(0);}", _constructor(null, AccessPermission.PROTECTED, l(), l(), l(_call("this2 ( 0 )"))));
        assertEqualsMember("C() {super(2, 3);}", _constructorDelegate(null, AccessPermission.PROTECTED, l(), l(), SuperThis.SUPER, "2,3", l()));
        //TODO test: abstract methods, interface methods (incl default), generic methods (either fail or do our best)
    }
    
    @Test
    public void testFieldAndVar()
    {
        assertEqualsMember("int x;", _var(AccessPermission.PROTECTED, false, false, "int", "x", null));
        assertEqualsMember("public static final int CONST=7;", _var(AccessPermission.PUBLIC, true, true, "int", "CONST", filled("7")));
        assertEqualsMember("private final bool b = 7, c=false;",
            _var(AccessPermission.PRIVATE, false, true, "bool", "b", filled("7")),
            _var(AccessPermission.PRIVATE, false, true, "bool", "c", filled("false")));
        assertEqualsMember("public static String a = null, b, c=(String)false, d;",
                _var(AccessPermission.PUBLIC, true, false, "String", "a", filled("null")),
                _var(AccessPermission.PUBLIC, true, false, "String", "b", null),
                _var(AccessPermission.PUBLIC, true, false, "String", "c", filled("( String ) false")),
                _var(AccessPermission.PUBLIC, true, false, "String", "d", null));

        assertEquals("int x;", _var(null, false, false, "int", "x", null));
        assertEquals("final int CONST=7;", _var(null, false, true, "int", "CONST", filled("7")));
        assertEquals("bool b = 7, c=false;",
                _var(null, false, false, "bool", "b", filled("7")),
                _var(null, false, false, "bool", "c", filled("false")));
        assertEquals("final String a = null, b, c=(String)false, d;",
                _var(null, false, true, "String", "a", filled("null")),
                _var(null, false, true, "String", "b", null),
                _var(null, false, true, "String", "c", filled("( String ) false")),
                _var(null, false, true, "String", "d", null));
    }

    @Test
    public void testComments()
    {
        assertEquals("//ABC", _comment("ABC"));
        assertEquals("//Declares x\nint x;", _comment("Declares x"), _var(null, false, false, "int", "x", null));
        assertEquals("//Declares x\nint x /* empty */;", _comment("Declares x empty"), _var(null, false, false, "int", "x", null));
        assertEquals("//Declares x\nint x /* empty */;int y;", _comment("Declares x empty"), _var(null, false, false, "int", "x", null), _var(null, false, false, "int", "y", null));
        assertEquals("//Declares x\nint x;int y/* empty */;", _comment("Declares x"), _var(null, false, false, "int", "x", null), _comment("empty"), _var(null, false, false, "int", "y", null));
        
        assertEquals("break; // Post-comment", new BreakElement(null, true), _comment("Post-comment"));
        assertEquals("while(true) {/*Just-comment*/}", _while("true", _comment("Just-comment")));
        assertEquals("while(true) {break; /*End-comment*/}", _while("true", new BreakElement(null, true), _comment("End-comment")));
        assertEquals("while(true) {break; }/*After-comment*/", _while("true", new BreakElement(null, true)), _comment("After-comment"));
        assertEquals("return 0; /*XXX*/ while(true) {}", _return("0"), _comment("XXX"), _while("true"));
    }
    
    @Test
    public void testWhole()
    {
        assertEqualsFile("class Foo {}", _class(null, l(), null, false, "Foo", null, l(), l(), l(), l()));
        assertEqualsFile("abstract class A extends B { int x; }",
            _class(null, l(), null, true, "A", "B", l(), l(_var(AccessPermission.PROTECTED, false, false, "int", "x", null)), l(), l()));
        
        // TODO more tests
    }
    
    @Test
    public void testRandomStatement()
    {
        for (int i = 0; i < 10000; i++)
        {
            roundTripStatement(several(() -> genStatement(3)));
        }
    }
    
    private static CodeElement genStatement(int maxDepth)
    {
        List<Supplier<CodeElement>> terminals = Arrays.asList(
            () -> new CommentElement("c" + rand(0, 9)),
            () -> new BreakElement(null, true),
            () -> new ReturnElement(null, genOptExpression(), true),
            () -> new ThrowElement(null, genExpression(), true)
        );
        List<Supplier<CodeElement>> all = new ArrayList<>(Arrays.asList(
            () -> new WhileElement(null, genExpression(), some(() -> genStatement(maxDepth - 1)), true)
                //TODO if, try, etc
        ));
        all.addAll(terminals);
        //TODO call/assignment
        if (maxDepth <= 1)
            return oneOf(terminals.toArray(new Supplier[0]));
        else
            return oneOf(all.toArray(new Supplier[0]));
    }

    private static OptionalExpressionSlotFragment genOptExpression()
    {
        return oneOf(() -> null, () -> new OptionalExpressionSlotFragment(genExpression()));
    }

    private static FilledExpressionSlotFragment genExpression()
    {
        return oneOf(
            () -> new FilledExpressionSlotFragment("0", "0")
        );
    }

    private static <T> T oneOf(Supplier<T>... items)
    {
        return items[rand(0, items.length - 1)].get();
    }
    
    private static <T> List<T> collapseComments(List<T> items)
    {
        ArrayList<T> r = new ArrayList<>(items);
        int i = 1;
        while (i < r.size())
        {
            // We don't try two comment elements next to each other:
            if (r.get(i - 1) instanceof CommentElement && r.get(i) instanceof CommentElement)
                r.remove(i);
            else
                i += 1;
        }
        return r;
    }
    
    private static <T> List<T> some(Supplier<T> item)
    {
        return collapseComments(Stream.generate(item).limit(rand(0, 4)).collect(Collectors.toList()));
    }

    private static <T> List<T> several(Supplier<T> item)
    {
        return collapseComments(Stream.generate(item).limit(rand(1, 3)).collect(Collectors.toList()));
    }
    
    private static int rand(int min, int max)
    {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    private static void roundTripStatement(List<CodeElement> original)
    {
        String java = collapseComments(original).stream().map(el -> el.toJavaSource().toTemporaryJavaCodeString()).collect(Collectors.joining("\n"));
        test(java, original.toArray(new CodeElement[0]), Parser.JavaContext.STATEMENT);
    }

    private ClassElement _class(String pkg, List<String> imports, String javadoc, boolean _abstract, String name, String _extends, List<String> _implements, List<VarElement> fields, List<ConstructorElement> constructors, List<NormalMethodElement> methods)
    {
        return new ClassElement(null, null, _abstract, new NameDefSlotFragment(name), _extends == null ? null : new TypeSlotFragment(_extends, _extends),
            _implements.stream().map(t -> new TypeSlotFragment(t, t)).collect(Collectors.toList()),
            fields, constructors, methods, new JavadocUnit(javadoc), pkg == null ? null : new PackageFragment(pkg), imports.stream().map(i -> new ImportElement(i, null, true)).collect(Collectors.toList()), true);
    }

    private CommentElement _comment(String s)
    {
        return new CommentElement(s);
    }

    private VarElement _var(AccessPermission access, boolean _static, boolean _final, String type, String name, FilledExpressionSlotFragment init)
    {
        return new VarElement(null, access == null ? null : new AccessPermissionFragment(access), _static, _final, new TypeSlotFragment(type, type), new NameDefSlotFragment(name), init, true);
    }
    
    private ParamFragment _param(String type, String name)
    {
        return new ParamFragment(new TypeSlotFragment(type, type), new NameDefSlotFragment(name));
    }

    private CodeElement _method(String comment, AccessPermission accessPermission, boolean _static, boolean _final, String returnType, String name, List<ParamFragment> params, List<String> _throws, List<CodeElement> body)
    {
        return new NormalMethodElement(null, new AccessPermissionFragment(accessPermission), _static, _final, new TypeSlotFragment(returnType, returnType), new NameDefSlotFragment(name), params, _throws.stream().map(t -> new ThrowsTypeFragment(new TypeSlotFragment(t, t))).collect(Collectors.toList()), body, new JavadocUnit(comment), true);
    }

    private CodeElement _constructor(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(new TypeSlotFragment(t, t))).collect(Collectors.toList()), null, null, body, new JavadocUnit(comment), true);
    }

    private CodeElement _constructorDelegate(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, SuperThis superThis, String superThisArgs, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(new TypeSlotFragment(t, t))).collect(Collectors.toList()), new SuperThisFragment(superThis), new SuperThisParamsExpressionFragment(superThisArgs, superThisArgs), body, new JavadocUnit(comment), true);
    }

    private CodeElement _constructorDelegate(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, SuperThis superThis, SuperThisParamsExpressionFragment superThisArgs, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(new TypeSlotFragment(t, t))).collect(Collectors.toList()), new SuperThisFragment(superThis), superThisArgs, body, new JavadocUnit(comment), true);
    }

    private static void assertExpression(String expectedStride, String original)
    {
        Assert.assertEquals(expectedStride, JavaStrideParser.replaceInstanceof(original));
    }

    private CallElement _call(String call)
    {
        return _call(new CallExpressionSlotFragment(call, call));
    }

    private CallElement _call(CallExpressionSlotFragment call)
    {
        return new CallElement(null, call, true);
    }

    private ReturnElement _return()
    {
        return new ReturnElement(null, null, true);
    }

    private ReturnElement _return(String s)
    {
        return _return(new OptionalExpressionSlotFragment(s, s));
    }

    private ReturnElement _return(OptionalExpressionSlotFragment s)
    {
        return new ReturnElement(null, s, true);
    }

    private WhileElement _while(String expression, CodeElement... body)
    {
        return _while(filled(expression), body);
    }

    private WhileElement _while(FilledExpressionSlotFragment expression, CodeElement... body)
    {
        return new WhileElement(null, expression, Arrays.asList(body), true);
    }

    // If without any elses
    private IfElement _if(String expression, CodeElement... body)
    {
        return _ifElse(expression, Arrays.asList(body), null);
    }

    // If with an else
    private IfElement _ifElse(String expression, List<CodeElement> body, List<CodeElement> elseBody)
    {
        return _ifElseIf(expression, body, Collections.emptyList(), Collections.emptyList(), elseBody);
    }

    private IfElement _ifElseIf(String expression, List<CodeElement> body, List<String> expressions, List<List<CodeElement>> elseIfBodies, List<CodeElement> elseBody)
    {
        return new IfElement(null, filled(expression), body, expressions.stream().map(this::filled).collect(Collectors.toList()), elseIfBodies, elseBody, true);
    }

    private IfElement _ifElseIf(FilledExpressionSlotFragment expression, List<CodeElement> body, List<FilledExpressionSlotFragment> expressions, List<List<CodeElement>> elseIfBodies, List<CodeElement> elseBody)
    {
        return new IfElement(null, expression, body, expressions, elseIfBodies, elseBody, true);
    }
    
    private FilledExpressionSlotFragment filled(String e)
    {
        return new FilledExpressionSlotFragment(e, e);
    }
        
    // Short-hand for Arrays.asList
    private static <T> List<T> l(T... items)
    {
        return Arrays.asList(items);
    }

    private static void assertEquals(String javaSource, CodeElement... expectedStride)
    {
        test(javaSource, expectedStride, Parser.JavaContext.STATEMENT);
        roundTripStatement(Arrays.asList(expectedStride));
    }

    private static void assertEqualsMember(String javaSource, CodeElement... expectedStride)
    {
        test(javaSource, expectedStride, Parser.JavaContext.CLASS_MEMBER);
    }

    private static void assertEqualsFile(String javaSource, CodeElement... expectedStride)
    {
        test(javaSource, expectedStride, Parser.JavaContext.TOP_LEVEL);
    }

    private static void test(String javaSource, CodeElement[] expectedStride, Parser.JavaContext classMember)
    {
        List<CodeElement> result = Parser.javaToStride(javaSource, classMember);
        List<String> resultXML = result.stream().map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        List<String> expectedXML = Arrays.stream(expectedStride).map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        Assert.assertEquals("Checking XML", expectedXML, resultXML);
    }
}