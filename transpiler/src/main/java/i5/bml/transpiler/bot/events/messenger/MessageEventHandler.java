package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.events.EventHandler;

public class MessageEventHandler {

    private MessageEventHandler() {}

    /**
     * @param context see {@link MessageEventContext} for more detailed information.
     *                This is the equivalent of the information that is provided in
     *                BML to an event listener.
     */
    @EventHandler(messageEventType = MessageEventType.USER_STARTED_CHAT)
    public static void welcomeUser(MessageEventContext context) {
//        MessageHelper.replyToMessenger(context, "Oh hi there! \uD83D\uDC40");
    }

    /**
     * Uses all messenger components. That is, receiving a message on Telegram
     * will result in an answer on Telegram. It is not possible to receive on one
     * messenger and send to another one. This could be possible with Slack since it
     * allows addressing users and channels directly, but Telegram for example does not
     * allow this.
     *
     * @param context see {@link MessageEventContext} for more detailed information.
     *                This is the equivalent of the information that is provided in
     *                BML to an event listener.
     */
    @EventHandler(messageEventType = MessageEventType.BOT_COMMAND)
    @EventHandler(messageEventType = MessageEventType.USER_SENT_MESSAGE)
    public static void collectPetId(MessageEventContext context) {
//        if (context.getIntent().equals("id")) {
//            long id = Long.parseLong(context.getEntity());
//
//            int code = 200;
//            Pet pet = null;
//            try {
//                pet = ComponentRegistry.getPetAPI().getPetById(1L);
//            } catch (ApiException e) {
//                code = e.getCode();
//            }
//
//            if (code == 200 && pet != null) {
//                // If not specified otherwise, send back to user that message was received from
//                // send uses all "registered" messengers (see @UserSentMessage)
//                var msg = "Thank you, I'll keep you posted about %s with id %s".formatted(pet.getName(), pet.getId());
//                MessageHelper.replyToMessenger(context, msg);
//                ComponentRegistry.getSubscribed().put(context.getEvent().user, Long.parseLong(context.getEntity()));
//            } else if (code == 400) {
//                var msg = "I'm sorry, it seems like you provided an invalid id." +
//                        "Please make sure that you are providing a valid number.";
//                MessageHelper.replyToMessenger(context, msg);
//            } else { // code == 400
//                MessageHelper.replyToMessenger(context, "I'm sorry, it seems there is no pet with id %s.".formatted(id));
//            }
//        } else {
//            /* Do some dialogue to help user send a pet id */
//            MessageHelper.replyToMessenger(context, "I'm sorry I didn't get that");
//        }
    }
}
