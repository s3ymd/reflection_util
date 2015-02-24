import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ReflectionUtil {

	private ClassCache classCache = new ClassCache();
	private ConstructorCache constructorCache = new ConstructorCache();

	public <T> T invokeConstructor(Class<T> cls, Object... args) {
		Constructor<T> c = constructorCache.get(cls, args);
		if (c == null) {
			throw new RuntimeException("Constructor not found: " + cls);
		}
		Object[] newArgs = c.isVarArgs() ? makeVarArgArray(c, args) : args;
		try {
			return c.newInstance(newArgs);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Object invokeClassMethod(Class<?> cls, String methodName,
			Object... args) {
		return invokeMethod(cls, null, methodName, args);
	}

	public Object invokeInstanceMethod(Object obj, String methodName,
			Object... args) {
		return invokeMethod(obj.getClass(), obj, methodName, args);
	}

	public Object invokeMethod(Class<?> cls, Object obj, String methodName,
			Object... args) {
		Method m = classCache.find(cls, methodName, args);
		if (m == null) {
			throw new RuntimeException("Method not found: " + methodName);
		}
		Object[] newArgs = m.isVarArgs() ? makeVarArgArray(m, args) : args;
		try {
			return m.invoke(obj, newArgs);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Object[] makeVarArgArray(Executable m, Object[] args) {
		int paramCount = m.getParameterCount();
		Object[] newArgs = new Object[paramCount];
		for (int i = 0; i < paramCount - 1; i++) {
			newArgs[i] = args[i];
		}

		int varArgsLength = args.length - paramCount + 1;
		Class<?> lastType = m.getParameterTypes()[paramCount - 1]
				.getComponentType();
		Object varArgs = Array.newInstance(lastType, varArgsLength);
		for (int i = 0; i < varArgsLength; i++) {
			Array.set(varArgs, i, args[i + paramCount - 1]);
		}
		newArgs[paramCount - 1] = varArgs;

		return newArgs;
	}

	private Executable findMatchExecutable(Executable[] executables,
			Object[] args) {
		for (Executable e : executables) {

			if (e.isVarArgs()) {
				if (varArgExecutableMatches(e, args)) {
					return e;
				}
			} else {
				if (nonVarArgExecutableMatches(e, args)) {
					return e;
				}
			}
		}
		return null;
	}

	private boolean nonVarArgExecutableMatches(Executable m, Object[] args) {

		Class<?>[] types = m.getParameterTypes();
		if (types.length != args.length) {
			return false;
		}

		for (int i = 0; i < types.length; i++) {
			if (!typeMatches(types[i], args[i])) {
				return false;
			}
		}

		return true;
	}

	private boolean typeMatches(Class<?> type, Object arg) {
		if (type.isPrimitive()) {
			return primitiveTypeMatches(type, arg);
		} else if (arg == null) {
			return true;
		} else {
			return type.isAssignableFrom(arg.getClass());
		}

	}

	private boolean primitiveTypeMatches(Class<?> type, Object arg) {
		if (arg == null) {
			return false;
		} else if (type == Boolean.TYPE) {
			return arg instanceof Boolean;
		} else if (type == Integer.TYPE) {
			return arg instanceof Integer;
		} else if (type == Double.TYPE) {
			return arg instanceof Double;
		} else if (type == Character.TYPE) {
			return arg instanceof Character;
		} else if (type == Long.TYPE) {
			return arg instanceof Long;
		} else if (type == Short.TYPE) {
			return arg instanceof Short;
		} else if (type == Float.TYPE) {
			return arg instanceof Float;
		} else if (type == Byte.TYPE) {
			return arg instanceof Byte;
		} else {
			return false;
		}
	}

	private boolean varArgExecutableMatches(Executable m, Object[] args) {
		Class<?>[] types = m.getParameterTypes();
		int requiredArgsCount = types.length - 1;
		if (args.length < requiredArgsCount) {
			return false;
		}

		for (int i = 0; i < requiredArgsCount; i++) {
			if (!typeMatches(types[i], args[i])) {
				return false;
			}
		}

		if (args.length < types.length) {
			return true;
		}

		Class<?> lastType = types[types.length - 1].getComponentType();
		for (int i = types.length - 1; i < args.length; i++) {
			if (!typeMatches(lastType, args[i])) {
				return false;
			}
		}
		return true;
	}

	// Class -> Constructors[]
	private class ConstructorCache {
		private Map<Class<?>, Constructor<?>[]> map = new HashMap<>();

		@SuppressWarnings("unchecked")
		public <T> Constructor<T> get(Class<T> cls, Object... args) {
			Constructor<?>[] constructors = map.get(cls);
			if (constructors == null) {
				constructors = getConstructors(cls);
				map.put(cls, constructors);
			}
			return (Constructor<T>) findMatchExecutable(constructors, args);
		}

		private Constructor<?>[] getConstructors(Class<?> cls) {
			List<Constructor<?>> constructors = new LinkedList<>();
			Constructor<?>[] cs = cls.getConstructors();
			Arrays.sort(cs, EXECUTABLE_COMPARATOR);

			for (Constructor<?> c : cs) {
				constructors.add(c);
			}
			return (Constructor<?>[]) constructors
					.toArray(new Constructor<?>[constructors.size()]);
		}
	}

	// Class -> MethodCache
	private class ClassCache {
		private Map<Class<?>, MethodCache> map = new HashMap<>();

		public Method find(Class<?> cls, String methodName, Object... args) {
			return get(cls).get(methodName, args);
		}

		private MethodCache get(Class<?> cls) {
			MethodCache cm = map.get(cls);
			if (cm == null) {
				cm = new MethodCache(cls);
				map.put(cls, cm);
			}
			return cm;
		}

	}

	// (method-name, method-arguments) -> Method[]
	private class MethodCache {
		private Class<?> cls;
		private Map<String, Method[]> map = new HashMap<>();

		public MethodCache(Class<?> cls) {
			this.cls = cls;
		}

		public Method get(String methodName, Object... args) {
			Method[] methods = map.get(methodName);
			if (methods == null) {
				methods = getMethods(methodName);
				map.put(methodName, methods);
			}
			return (Method) findMatchExecutable(methods, args);
		}

		private Method[] getMethods(String methodName) {
			List<Method> methods = new LinkedList<>();
			for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
				Method[] ms = c.getDeclaredMethods();
				Arrays.sort(ms, EXECUTABLE_COMPARATOR);
				for (Method m : ms) {
					if (methodName.equals(m.getName())) {
						methods.add(m);
					}
				}
			}
			return (Method[]) methods.toArray(new Method[methods.size()]);
		}
	}

	private static final Comparator<Executable> EXECUTABLE_COMPARATOR = new Comparator<Executable>() {
		private static final int WEIGHT = 100;

		@Override
		public int compare(Executable e1, Executable e2) {
			int p1 = e1.getParameterCount() * WEIGHT + (e1.isVarArgs() ? 1 : 0);
			int p2 = e2.getParameterCount() * WEIGHT + (e2.isVarArgs() ? 1 : 0);
			return p1 - p2;
		}

	};

}
