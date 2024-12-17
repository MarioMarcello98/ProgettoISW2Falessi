package retrievers;
import entities.Ticket;
import java.util.ArrayList;

public class Proportion {
    // Predicted IV = FV - (FV - OV) * P
    public static void estimateInjectedVersion(ArrayList<Ticket> tickets, float proportion) {
        for (Ticket ticket : tickets) {
            if (ticket.proportion == 0) {
                //ticket.injectedVersion = Math.max(0, Math.round(ticket.fixVersion - (ticket.fixVersion - ticket.openingVersion) * proportion));
            }
        }
    }



}