package reflection.testee;

public class Test59 {
	public static void main(String[] args) {
		try {
			String s = null;
			if(args.length == 0)
				s = "reflection.testee.Test59A";
			else
				s = "reflection.testee.Test59A";
			Class clz = Class.forName(s);
			System.out.println(clz.newInstance());
		} catch (Exception e) {
			
		}
	}
}
