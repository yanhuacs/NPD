package testcase;


public class LinkedList1 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Node_1 cur = new Node_1();
		for (int k = 0; k < 5; ++k) { 
			System.out.printf("%d", cur.val);
			cur.nxt = new Node_1();
			cur = cur.nxt; //safe
			cur.nxt = null; //safe
		}
		for (int k = 0; k < 5; ++k) { 
			System.out.printf("%d", cur.val); //safe
			cur = cur.nxt; //safe
		}
		System.out.printf("%d", cur.val); //bug
		cur = cur.nxt; //bug
	}
}

class Node_1 //Linked list data structure.
{
	int val;
	Node_1 nxt;
}