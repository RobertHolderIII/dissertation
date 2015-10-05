import java.util.TreeSet;


public class TreeSetTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TreeSet<Data> t = new TreeSet<Data>();

		t.add(new Data(1));
		t.add(new Data(1));
		t.add(new Data(2));

		System.out.println(t.toString());

		String openTag = "<someString>";
		String closeTag = "</someString>";

		String newOpenTag = openTag.replaceAll("<(/?)someString>",
												"<$1SomeString>");
		System.out.println(newOpenTag);

		String newCloseTag = closeTag.replaceAll("<(/?)someString>",
		"<$1SomeString>");
System.out.println(newCloseTag);
	}

	private static class Data implements Comparable<Data>{
		int data;
		public Data(int data){
			this.data = data;
		}
		public int compareTo(Data o){
			return this.data = o.data;
		}
	}
}
