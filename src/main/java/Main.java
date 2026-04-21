import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bson.Document;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {

    public static JDA jda;
    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    
    // --- MONGODB CONFIGURATION ---
    private static final String MONGO_URI = "mongodb+srv://runestack:m209874hu@cluster0.05p8wfv.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> usersCollection;

    // --- DISCORD BOT TOKEN ---
    // Yahan apna bot token paste karein
    private static final String BOT_TOKEN = "PASTE_YOUR_BOT_TOKEN_HERE";

    public static void main(String[] args) {
        try {
            // 1. Initialize MongoDB
            mongoClient = MongoClients.create(MONGO_URI);
            database = mongoClient.getDatabase("RuneStackDB");
            usersCollection = database.getCollection("users");
            System.out.println("✅ MongoDB Atlas Connected!");

            // 2. Initialize JDA (Discord Bot)
            jda = JDABuilder.createDefault(BOT_TOKEN)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, 
                                   GatewayIntent.MESSAGE_CONTENT, 
                                   GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(new Flowerpoker(), new Hotcold())
                    .setActivity(Activity.playing("RuneStack Casino"))
                    .build();

            jda.awaitReady();
            System.out.println("✅ RuneStack Bot is Online!");

        } catch (Exception e) {
            System.err.println("❌ Error starting the bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- DATABASE HELPER METHODS ---

    public static UserData getUserData(String userId) {
        Document doc = usersCollection.find(Filters.eq("_id", userId)).first();
        if (doc == null) {
            // Agar user naya hai toh database mein create karein
            usersCollection.insertOne(new Document("_id", userId)
                    .append("balance", 0.0)
                    .append("wagered", 0.0)
                    .append("rakeback", 0.0));
            return new UserData();
        }
        UserData data = new UserData();
        data.balance = doc.getInternalStringOrDouble("balance"); // Helper to handle numeric types
        data.wagered = doc.getInternalStringOrDouble("wagered");
        data.rakeback = doc.getInternalStringOrDouble("rakeback");
        return data;
    }

    public static void saveUserData(String userId, UserData data) {
        usersCollection.updateOne(Filters.eq("_id", userId),
                Updates.combine(
                        Updates.set("balance", data.balance),
                        Updates.set("wagered", data.wagered),
                        Updates.set("rakeback", data.rakeback)
                ));
    }

    public static void updateWagerAndRakeback(String userId, double amount) {
        double rbIncrease = amount * 0.006; // 0.6% Rakeback calculation
        usersCollection.updateOne(Filters.eq("_id", userId),
                Updates.combine(
                        Updates.inc("wagered", amount),
                        Updates.inc("rakeback", rbIncrease)
                ));
    }

    // Internal class to map user data easily
    public static class UserData {
        public double balance = 0.0;
        public double wagered = 0.0;
        public double rakeback = 0.0;
    }
}
