import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.zecdev.zip321.ZIP321;
import org.zecdev.zip321.ZIP321.Errors;

public class ZIP321Fuzzer {

	public static void fuzzerInitialize() {
		// Nothing to do
	}

	public static void fuzzerTestOneInput(FuzzedDataProvider data) {
		String request = data.consumeRemainingAsString();
		try {
			ZIP321.INSTANCE.request(request, null);
		} catch (Errors e) {
			// Allowed
		}
	}
}
