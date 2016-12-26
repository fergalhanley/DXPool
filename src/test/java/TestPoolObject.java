
public class TestPoolObject {

	private static int objectCount = 0;

	public static TestPoolObject generateNewInstance() throws InterruptedException {
		Thread.sleep(400);
		objectCount++;
		System.out.println("generated object " + objectCount);
		return new TestPoolObject();
	}

	public String doTheThing(){
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "did the thing";
	}
}
