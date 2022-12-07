package i5.bml.transpiler.bot.events.routines;

import i5.bml.transpiler.bot.ComponentRegistry;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;

public class RoutineEventHandler {

    public void reportPetStatus(RoutineEventContext context) {
        try {
//            for (var entry : ComponentRegistry.getSubscribed().entrySet()) {
//                Pet pet = ComponentRegistry.getPetAPI().getPetById(entry.getValue());
//
//                String msg = "%s:\n %s is **%s**!".formatted(1, pet.getName(), pet.getStatus());
//                MessageHelper.replyToMessenger(entry.getKey(), msg);
//            }
        } catch (Exception e) {
            // TODO: Proper handling
            e.printStackTrace();
        }
    }
}
