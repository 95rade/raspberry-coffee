package http;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.fail;

public class HTTPServerTests {

	@Test
	public void detectBadRequestManager() {
		List<HTTPServer.Operation> opList1 = Arrays.asList(
				new HTTPServer.Operation(
						"GET",
						"/oplist",
						this::emptyOperation,
						"List of all available operations."),
				new HTTPServer.Operation(
						"POST",
						"/create/{it}",
						this::emptyOperation,
						"List of all available operations."),
				new HTTPServer.Operation(
						"POST",
						"/terminate",
						this::emptyOperation,
						"Hard stop, shutdown. VERY unusual REST resource..."));


		List<HTTPServer.Operation> opList2 = Arrays.asList(
				new HTTPServer.Operation(
						"GET",
						"/oplist",
						this::emptyOperation,
						"List of all available operations."),
				new HTTPServer.Operation(
						"POST",
						"/create/{this}",
						this::emptyOperation,
						"List of all available operations."),
				new HTTPServer.Operation(
						"POST",
						"/finish",
						this::emptyOperation,
						"Hard stop, shutdown. VERY unusual REST resource..."));

		 HTTPServerInterface restServerImplOne = new HTTPServerInterface() {

			 @Override
			 public HTTPServer.Response onRequest(HTTPServer.Request request) throws UnsupportedOperationException {
				 return null;
			 }

			 @Override
			 public List<HTTPServer.Operation> getRESTOperationList() {
				 return opList1;
			 }
		 };


		HTTPServerInterface restServerImplTwo = new HTTPServerInterface() {

			@Override
			public HTTPServer.Response onRequest(HTTPServer.Request request) throws UnsupportedOperationException {
				return null;
			}

			@Override
			public List<HTTPServer.Operation> getRESTOperationList() {
				return opList2;
			}
		};
		HTTPServer httpServer = null;
		try {
			httpServer = new HTTPServer(restServerImplOne);
		} catch (Exception ex) {
			fail(ex.toString());
		}
		assertNotNull(httpServer);
		try {
			httpServer.addRequestManager(restServerImplTwo);
			fail("We should not be there");
		} catch (IllegalArgumentException ex) {
			System.out.println(String.format("As expected [%s]", ex.toString()));
		}
	}

	private HTTPServer.Response emptyOperation(HTTPServer.Request request) {
		HTTPServer.Response response = new HTTPServer.Response(request.getProtocol(), HTTPServer.Response.STATUS_OK);

		return response;
	}
}
