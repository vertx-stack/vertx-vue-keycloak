package backend;

import java.net.URI;
import java.net.URISyntaxException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

public class MainVerticle extends AbstractVerticle {

	// configuration for HTTP server
	private JsonObject httpServer = new JsonObject()
			.put("hostname", "0.0.0.0")
			.put("port", 8080);

	// configuration for HTTPS server
	private JsonObject httpsServer = new JsonObject()
			.put("hostname", "0.0.0.0")
			.put("port", 4443)
			.put("keyStore", "test.jks")
			.put("enforceRedirect", false);

	private JsonArray messages = new JsonArray().add("message1").add("message2");
	
	@Override
	public void start(Future<Void> startFuture) {
		createHttpServerAndRoutes();
		createApiEndpoints();
	}


	@Override
	public void stop() throws Exception {
		super.stop();
	}


	private void createHttpServerAndRoutes()	{
		Router router = Router.router(vertx);

		// create HTTP server
		HttpServerOptions httpOptions = new HttpServerOptions();
		httpOptions.setHost(httpServer.getString("hostname"));
		httpOptions.setPort(httpServer.getInteger("port"));
		httpOptions.setSsl(false);

		vertx.createHttpServer(httpOptions).requestHandler(router::accept).listen();
		System.out.println("created HTTP server at " + httpServer.getString("hostname") + ":" + httpServer.getInteger("port"));

		// create HTTPS server 
		HttpServerOptions httpsOptions = new HttpServerOptions();
		httpsOptions.setHost(httpsServer.getString("hostname"));
		httpsOptions.setPort(httpsServer.getInteger("port"));
		httpsOptions.setSsl(true);
		httpsOptions.setKeyStoreOptions(new JksOptions().setPath( httpsServer.getString("keyStore") ).setPassword("testpassword"));

		vertx.createHttpServer(httpsOptions).requestHandler(router::accept).listen();
		System.out.println("created HTTPS server at " + httpsServer.getString("hostname") + ":" + httpsServer.getInteger("port") + " (keyFile: " + httpsServer.getString("keyStore") + ")");

		boolean enforceSslRedirect = httpsServer.getBoolean("enforceRedirect");

		final BodyHandler bodyHandler = BodyHandler.create();
		router.route("/app/*").handler(bodyHandler);
		router.route("/login").handler(bodyHandler);

		// enable CORS
		router.route().handler(CorsHandler.create("http://localhost:8081")
				.allowedMethod(io.vertx.core.http.HttpMethod.GET)
				.allowedMethod(io.vertx.core.http.HttpMethod.POST)
				.allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
				.allowCredentials(true)
				.allowedHeader("Access-Control-Allow-Headers")
				.allowedHeader("Authorization")
				.allowedHeader("Access-Control-Allow-Method")
				.allowedHeader("Access-Control-Allow-Origin")
				.allowedHeader("Access-Control-Allow-Credentials")
				.allowedHeader("Content-Type"));
		
		// HTTP to HTTPS redirect
		router.route().handler( context -> {
			boolean sslUsed = context.request().isSSL();

			if(!sslUsed && enforceSslRedirect) {
				try {
					int httpsPort = httpsServer.getInteger("port");

					URI myHttpUri = new URI( context.request().absoluteURI() );
					URI myHttpsUri = new URI("https", 
							myHttpUri.getUserInfo(), 
							myHttpUri.getHost(), 
							httpsPort,
							myHttpUri.getRawPath(), 
							myHttpUri.getRawQuery(), 
							myHttpUri.getRawFragment());
					context.response().putHeader("location", myHttpsUri.toString() ).setStatusCode(302).end();
				} catch(URISyntaxException ex) {
					ex.printStackTrace();
					context.next();
				}
			}
			else context.next();
		});

		JsonObject keycloakJson = new JsonObject()
				.put("realm", "master") // (1)
				.put("realm-public-key", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqZeGGDeEHmmUN4/UXh2gQD0yZEZirprsrdYK7GfcE1+QF9yfYfBrIv5cQUssFQKISVpbbLcoqYolsxcOvDyVFSQedHRsumOzqNZK38RHkidPMPrSNof5C3iMIHuXOCv/6exnLZvVoeYmkq42davYEz1tpSWzkZnlUMbRZFs1CfzLMM2rsAJWsO1/5zbDm0JhFl7EFUsTki72ihac1Q5zUUSFyf1jKUEkL7rrkYINjgAaQKktE8pnubc3Y44F5llY4YyU9/bqUWqMYDx868oiDcnoBpGGd4QrUMlbULZZLRqqUKK6iG1kHxDCJQ9gaCiJoELyAqXjnnO28OODQhxMHQIDAQAB") // (2)
				.put("auth-server-url", "http://127.0.0.1:38080/auth")
				.put("ssl-required", "external")
				.put("resource", "vertx-account") // (3)
				.put("credentials", new JsonObject().put("secret", "0c22e587-2ccb-4dd3-b017-5ff6a903891b")); // (4)

		OAuth2Auth oauth2 = KeycloakAuth.create(vertx, OAuth2FlowType.PASSWORD, keycloakJson);



		// We need a user session handler too to make sure the user is stored in the session between requests
		//		router.route().handler(UserSessionHandler.create(authProvider));

		//		router.route("/").handler(context -> {
		//			context.response().putHeader("location", "/app").setStatusCode(302).end();
		//		});


		//		router.route("/app*").handler(RedirectAuthHandler.create(authProvider, "/login.html"));
		//		router.route("/app*").failureHandler(context -> {
		//			context.failure().printStackTrace();
		//		});

		// handler to deliver the user info object
		router.route("/app/userinfo").handler(context -> {
			if (context.user() != null) {
				JsonObject userDetails = context.user().principal();
				userDetails.remove("password");
				userDetails.put("jsessionid", context.session().id());
				context.request().response().putHeader("Content-Type", "application/json");
				context.request().response().end(userDetails.encodePrettily());
			}
			else context.request().response().end(
					new JsonObject().put("error", "401").put("message", "user is not authenticated").encodePrettily()
					);
		});


		router.route().handler(BodyHandler.create());


		router.post("/login").produces("application/json").handler(rc -> {
			System.err.println("received body ::: '"+rc.getBodyAsString()+"'");
			JsonObject userJson = rc.getBodyAsJson();
			System.err.println("User ::: "+userJson.encode());

			oauth2.authenticate(userJson, res -> {
				if (res.failed()) {
					System.err.println("Access token error: {} " + res.cause().getMessage());
					rc.response().setStatusCode(HttpResponseStatus.UNAUTHORIZED.code()).end();
				} else {
					User user = res.result();
					System.out.println("Success: we have found user: "+user.principal().encodePrettily());
					rc.response().end(user.principal().encodePrettily());
				}
			});
		});

		//		// Handles the actual login
		//		router.post("/login").handler(FormLoginHandler.create(authProvider).setDirectLoggedInOKURL("/app/"));
		//
		//		// Implement logout
		//		router.route("/logout").handler(context -> {
		//			context.clearUser();
		//			context.setUser(null);
		//			context.session().destroy();
		//			context.setSession(null);
		//			System.out.println("session destroyed, should log out now");
		////			context.reroute("/");
		//		});

		// make sure the user is properly authenticated when using the eventbus, if not reject with 403
		// TEMPORARILY DISABLED : test with c3.munich
//		router.route("/eventbus/*").handler(ctx -> {
//			// we need to be logged in
//			if (ctx.user() == null) {
//				ctx.fail(403);
//			} else {
//				ctx.next();
//			}
//		});

		
		BridgeOptions options = new BridgeOptions();
		options.addInboundPermitted(new PermittedOptions());
		options.addOutboundPermitted(new PermittedOptions());
		SockJSHandlerOptions sockjsOptions = new SockJSHandlerOptions();
		//		sockjsOptions.setHeartbeatInterval(2000);
		sockjsOptions.setInsertJSESSIONID(true);

		//		router.route("/eventbus/*").handler(SockJSHandler.create(vertx, sockjsOptions).bridge(options));

		/**
		 * vertxbus message maniupulation:
		 * we will add a reference to the user ibmserial for anything that comes in or goes out
		 */
		router.route("/eventbus/*").handler(SockJSHandler.create(vertx, sockjsOptions).bridge(options, event -> {
//			if (event.getRawMessage() != null) {
//				JsonObject raw = event.getRawMessage();
//				raw.put("headers", new JsonObject().put("serial", event.socket().webUser().principal().getInteger("serial")));
//			}
			event.complete(true);
		}));


		router.route().handler(StaticHandler.create().setCachingEnabled(true));
	}


	private void createApiEndpoints() {

		vertx.eventBus().consumer("/api/messages", this::apiMessages);
	}


	private void apiMessages(Message<JsonObject> msg) {
		System.err.println("apiMessages called");
		JsonObject inputObject = msg.body();
		System.out.println(inputObject.encode());
		msg.reply(messages);
	}
}


//
//@Log
//public class MainVerticle extends AbstractVerticle {
//
//	@Override
//	public void start() {
//
//		final Router router = Router.router(vertx);
//
//		// secure only in production
//		if (!Boolean.getBoolean("vertx.development")) {
//			System.out.println("production mode");
//			SecurityConfig.addSecurity(router, vertx);
//		} else {
//			System.out.println("development mode");
//		}
//
//		// store post bodies in rc for all api calls
//		router.route(HttpMethod.POST, "/api/*").handler(BodyHandler.create());
//		// mount sub routers
//		router.mountSubRouter("/api/messages", new MessageController(vertx).getRouter());
//
//		// register sockjs bridge to event bus
//		router.route("/eventbus/*").handler(new SockJSBridge(vertx));
//
//		// disable cache for static asset reload
//		router.route("/*").handler(StaticHandler.create().setCachingEnabled(!Boolean.getBoolean("vertx.development")));
//
//		// enable http compression (e.g. gzip js)
//		final HttpServerOptions options = new HttpServerOptions().setCompressionSupported(true);
//
//		// create server
//		HttpServer httpServer = vertx.createHttpServer(options);
//		httpServer.requestHandler(router::accept).listen(Integer.getInteger("server.port", 8080),
//				System.getProperty("server.host", "0.0.0.0"));
//
//	}
//}
