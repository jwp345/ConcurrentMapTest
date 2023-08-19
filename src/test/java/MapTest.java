import com.example.concurrentMap.GetMap;
import com.example.concurrentMap.ModifyMap;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class MapTest {

    @Test
    public void concurrentTest() {
        ExecutorService service = Executors.newFixedThreadPool(2);

        ModifyMap modifyMap = new ModifyMap();
        GetMap getMap = new GetMap();

        service.execute(() -> {
            modifyMap.clearAndPut();
        });

        try {
            service.submit(() ->
                    assertEquals("3", getMap.get())
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }
}
