import com.github.fergalhanley.dxpool.DXPool;
import org.junit.Test;

import java.sql.DriverManager;

public class PoolTests {

	@SuppressWarnings("unchecked")
	@Test
	public void createPool() {

		DXPool.create("db").initialize(() -> DriverManager.getConnection(""));

		DXPool.create("my-connection-pool")
				.before(testPoolObj -> System.out.println("before"))
				.after(testPoolObj -> System.out.println("after"))
				.setMaxPoolSize(10)
				.initialize(TestPoolObject::generateNewInstance);


		for(int i = 0; i < 10; i++) {

			final int finalI = i;
			DXPool.with("my-connection-pool").execute(o -> {

				TestPoolObject tpo = (TestPoolObject)o;
				String result = tpo.doTheThing();
				System.out.println(result + finalI);

			});

		}

	}
}
