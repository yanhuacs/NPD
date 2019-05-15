package pta.utils;

public class Dummy {
	// dummy methods for testee
	public static void call(Object o) {
		// to avoid the compiler to prune the Object
	}

	public static <O> O getNull() {
		// to avoid the compiler to prune the Object containing null
		return null;
	}
	
	public static void mayAlias(Object a, Object b) {
		// to be detected by Analyzer
	}

	public static void notAlias(Object a, Object b) {
		// to be detected by Analyzer
	}
}
