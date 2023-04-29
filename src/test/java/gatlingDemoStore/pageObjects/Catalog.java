package gatlingDemoStore.pageObjects;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

// class to hold the catalog related actions
public final class Catalog{
    // feeder to provide categories endpoint in a random manner
    private static final FeederBuilder<String> categoryFeeder =
            csv("data/categories.csv").random();

    // feeder to provide product details endpoints in random
    private static final FeederBuilder<Object> prodDetailsFeeder =
            jsonFile("data/prodDetails.json").random();
    public static class Category{
        // LoadCategoriesPage
        public static final ChainBuilder view =
                feed(categoryFeeder)
                        .exec(
                                http("LoadCategoriesPage-#{categoryName}")
                                        .get("/category/#{categoryUrl}")
                                        .check(css("#CategoryName").isEL("#{categoryName}"))
                        );
    }
    public static class Product{
        // LoadProductDetailsPage
        public static final ChainBuilder view =
                feed(prodDetailsFeeder)
                        .exec(
                                http("LoadProductDetailsPage-#{name}")
                                        .get("/product/#{slug}")
                                        .check(css("#ProductDescription").isEL("#{description}"))
                        );
        // AddProductToCart
        public static final ChainBuilder addToCart =
                exec(view)
                        .exec(
                                http("AddProductToCart")
                                        .get("/cart/add/#{id}")
                                        .check(substring("items in your cart."))
                        )
                        .exec(session -> {
                                    double newTotalCartValue = session.getDouble("totalCartValue") + session.getDouble("price");
                                    System.out.println("newCartValue: " + newTotalCartValue);
                                    return session.set("totalCartValue", newTotalCartValue);
                                }
                        );
    }
}
