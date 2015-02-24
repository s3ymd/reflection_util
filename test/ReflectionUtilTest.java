import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class ReflectionUtilTest {

	private ReflectionUtil util;

	@Before
	public void setUp() throws Exception {
		util = new ReflectionUtil();
	}

	@Test
	public void testOverride() {
		assertEquals("Super",
				util.invokeInstanceMethod(new SuperTestClass(), "getName"));
		assertEquals("Sub",
				util.invokeInstanceMethod(new SubTestClass(), "getName"));
	}

	@Test
	public void testConstructor() {
		assertEquals(1, util.invokeConstructor(ConstructorTestClass.class)
				.getConstructorNo());
		assertEquals(2,
				util.invokeConstructor(ConstructorTestClass.class, "hello")
						.getConstructorNo());
		assertEquals(3, util.invokeConstructor(ConstructorTestClass.class, 1)
				.getConstructorNo());
		assertEquals(4, util
				.invokeConstructor(ConstructorTestClass.class, 2, 3)
				.getConstructorNo());
	}

	@Test
	public void testStaticMethod() {
		assertEquals(1,
				util.invokeClassMethod(ClassMethodTestClass.class, "method"));
		assertEquals(2, util.invokeClassMethod(ClassMethodTestClass.class,
				"method", "hello", "world"));
		assertEquals(3, util.invokeClassMethod(ClassMethodTestClass.class,
				"method", 1, 2));
		assertEquals(4, util.invokeClassMethod(ClassMethodTestClass.class,
				"method", 1, 2, 3, 4));
	}

	@Test
	public void testInstanceMethod() {
		assertEquals(1, util.invokeInstanceMethod(
				new InstanceMethodTestClass(), "method"));
		assertEquals(2, util.invokeInstanceMethod(
				new InstanceMethodTestClass(), "method", "hello", "world"));
		assertEquals(3, util.invokeInstanceMethod(
				new InstanceMethodTestClass(), "method", 1, 2));
		assertEquals(4, util.invokeInstanceMethod(
				new InstanceMethodTestClass(), "method", 1, 2, 3, 4));
	}

	@Test
	public void testVarArg() {
		assertEquals(1, util.invokeClassMethod(VarArgTestClass.class, "sum", 1));
		assertEquals(10, util.invokeClassMethod(VarArgTestClass.class, "sum",
				1, 2, 3, 4));
		assertEquals("", util.invokeClassMethod(VarArgTestClass.class, "join"));
		assertEquals("hello", util.invokeClassMethod(VarArgTestClass.class,
				"join", "h", "e", "l", "l", "o"));
	}

	@Test
	public void testPrimitiveTypes() {
		assertEquals("true @ 1 2 3 4 5.5 6.6", util.invokeClassMethod(
				WrapperClassTestClass.class, "method", true, '@', (byte) 1,
				(short) 2, (int) 3, (long) 4, (float) 5.5, (double) 6.6));
		assertEquals("true @ 1 2 3 4 5.5 6.6", util.invokeClassMethod(
				PrimitivesTestClass.class, "method", true, '@', (byte) 1,
				(short) 2, (int) 3, (long) 4, (float) 5.5, (double) 6.6));
	}

	@Test
	public void testUseList() {
		Object list = util.invokeConstructor(ArrayList.class);
		util.invokeInstanceMethod(list, "add", 1);
		util.invokeInstanceMethod(list, "add", 2);
		assertTrue(list instanceof ArrayList);
		assertEquals(2, util.invokeInstanceMethod(list, "size"));
	}

	@Test
	public void testUseString() {
		assertEquals("123", util.invokeClassMethod(String.class, "format",
				"%d%d%d", 1, 2, 3));
	}

}

class SuperTestClass {
	public String getName() {
		return "Super";
	}
}

class SubTestClass extends SuperTestClass {
	@Override
	public String getName() {
		return "Sub";
	}
}

class ConstructorTestClass {
	public int constructorNo;

	public ConstructorTestClass() {
		constructorNo = 1;
	}

	public ConstructorTestClass(String s) {
		constructorNo = 2;
	}

	public ConstructorTestClass(int x) {
		constructorNo = 3;
	}

	public ConstructorTestClass(int x, int y) {
		constructorNo = 4;
	}

	public int getConstructorNo() {
		return constructorNo;
	}
}

class ClassMethodTestClass {
	public static int method() {
		return 1;
	}

	public static int method(String... s) {
		return 2;
	}

	public static int method(int x, int y) {
		return 3;
	}

	public static int method(int x, int y, int... z) {
		return 4;
	}
}

class InstanceMethodTestClass {
	public int method() {
		return 1;
	}

	public int method(String... s) {
		return 2;
	}

	public int method(int x, int y) {
		return 3;
	}

	public int method(int x, int y, int... z) {
		return 4;
	}
}

class VarArgTestClass {
	public static int sum(int x, int... y) {
		int sum = x;
		for (int n : y) {
			sum += n;
		}
		return sum;
	}

	public static String join(String... args) {
		StringBuilder sb = new StringBuilder();
		for (String s : args) {
			sb.append(s);
		}
		return sb.toString();
	}
}

class WrapperClassTestClass {
	public static String method(Boolean p1, Character p2, Byte p3, Short p4,
			Integer p5, Long p6, Float p7, Double p8) {
		return String.format("%b %c %d %d %d %d %1.1f %1.1f", p1, p2, p3, p4,
				p5, p6, p7, p8);
	}
}

class PrimitivesTestClass {
	public static String method(boolean p1, char p2, byte p3, short p4, int p5,
			long p6, float p7, double p8) {
		return String.format("%b %c %d %d %d %d %1.1f %1.1f", p1, p2, p3, p4,
				p5, p6, p7, p8);
	}

}
