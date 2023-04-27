package gatlingDemoStore;

//import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class DemoStoreSimulation extends Simulation {

  private static final String domainURL = "demostore.gatling.io";
  private static final String protocol = "https";

  private static final HttpProtocolBuilder httpProtocol = http.baseUrl(protocol + "://" + domainURL);

  // feeder to provide categories endpoint in a random manner
  private static final FeederBuilder<String> categoryFeeder =
  csv("data/categories.csv").random();

  // feeder to provide product details endpoints in random
  private static final FeederBuilder<Object> prodDetailsFeeder =
  jsonFile("data/prodDetails.json").random();

  // class to hold the CMS page actions
  private static class cmsPages{
    // LoadHomePage
    private static final ChainBuilder homePage =
      exec(
        http("LoadHomePage")
        .get("/")
        .check(regex("<title>Gatling Demo-Store</title>"))
        .check(css("#_csrf", "content").saveAs("csrfToken"))
      );
    
    // LoadAboutUsPage
    private static final ChainBuilder aboutUsPage =
      exec(
        http("LoadAboutUsPage")
        .get("/about-us")
        .check(substring("About Us"))
        //.check(regex("<h2>About Us</h2>"))
      );

  }

  // class to hold the catalog related actions
  private static class Catalog{
    private static class Category{
      // LoadCategoriesPage
      private static final ChainBuilder viewcategories =
        feed(categoryFeeder)
        .exec(
          http("LoadCategoriesPage - #{categoryName}")
          .get("/category/#{categoryUrl}")
          .check(css("#CategoryName").isEL("#{categoryName}"))
        );
    }

    private static class Product{
      // LoadProductDetailsPage
      private static final ChainBuilder prodDetailsPage =
        feed(prodDetailsFeeder)
        .exec(
          http("LoadProductDetailsPage-#{name}")
          .get("/product/#{slug}")
          .check(css("#ProductDescription").isEL("#{description}"))
        );
    }
  }

  // class to hold the cart related actions
  private static class Cart{
    // AddProductToCart
    private static final ChainBuilder addToCart =
    exec(
      http("AddProductToCart")
      .get("/cart/add/19")
    );

    // ViewCart
    private static final ChainBuilder viewCart =
    exec(
      http("ViewCart")
      .get("/cart/view")
    ); 

    // CheckoutCart
    private static final ChainBuilder checkoutCart =
      exec(
        http("CheckoutCart")
        .get("/cart/checkout")
      );
  }

  // class to hold the User related actions
  private static class User{
    // UserLogin
    private static final ChainBuilder login =
      exec(
        http("UserLogin")
        .post("/login")
        .formParam("_csrf", "#{csrfToken}")
        .formParam("username", "user1")
        .formParam("password", "pass")
      );
  } 

  private static final ScenarioBuilder scn = scenario("DemoStoreSimulation")
    .exec(cmsPages.homePage)
    .pause(2)
    .exec(cmsPages.aboutUsPage)
    .pause(2)
    .exec(Catalog.Category.viewcategories)
    .pause(2)
    .exec(Catalog.Product.prodDetailsPage)
    .pause(2)
    .exec(Cart.addToCart)
    .pause(2)
    .exec(Cart.viewCart)
    .pause(2)
    .exec(User.login)
    .pause(2)
    .exec(Cart.checkoutCart);

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
