package i5.bml.transpiler.bot;

import com.slack.api.socket_mode.SocketModeClient;
import i5.bml.transpiler.bot.events.messenger.User;
import i5.bml.transpiler.bot.openapi.petstore3client.apis.PetApi;
import i5.bml.transpiler.bot.openapi.petstore3client.apis.StoreApi;
import i5.bml.transpiler.bot.openapi.petstore3client.apis.UserApi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ComponentRegistry {

    private static SocketModeClient slackComponent;

    public static SocketModeClient getSlackComponent() {
        return slackComponent;
    }

    public static void setSlackComponent(SocketModeClient slackComponent) {
        ComponentRegistry.slackComponent = slackComponent;
    }

    private static final ConcurrentHashMap<User, Long> subscribed = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<User, Long> getSubscribed() {
        return subscribed;
    }

    /**
     *
     */
    private static final ThreadLocal<PetApi> petAPI = ThreadLocal.withInitial(PetApi::new);

    /**
     *
     */
    public static PetApi getPetAPI() {
        return petAPI.get();
    }

    /**
     *
     */
    private static final ThreadLocal<StoreApi> storeAPI = ThreadLocal.withInitial(StoreApi::new);

    public static StoreApi getStoreAPI() {
        return storeAPI.get();
    }

    /**
     *
     */
    private static final ThreadLocal<UserApi> userAPI = ThreadLocal.withInitial(UserApi::new);

    public static UserApi getUserApi() {
        return userAPI.get();
    }
}
