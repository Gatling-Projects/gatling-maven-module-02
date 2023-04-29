package gatlingDemoStore.pageObjects;

import io.gatling.javaapi.core.ChainBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

// class to hold the cart related actions
public final class Cart{
    // ViewCart
    public static final ChainBuilder viewCart =
            doIf(session -> !session.getBoolean("customerLoggedIn"))
                    .then(exec(User.login))
                    .exec(
                            http("ViewCart")
                                    .get("/cart/view")
                                    .check(css("#grandTotal").isEL("$#{totalCartValue}"))
                    );
    // CheckoutCart
    public static final ChainBuilder checkoutCart =
            exec(
                    http("CheckoutCart")
                            .get("/cart/checkout")
                            .check(substring("Thanks for your order! See you soon!"))
            );
}
