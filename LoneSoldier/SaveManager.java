import java.io.*;

public class SaveManager {
    private static final String FILE = "last_outpost_save.dat";

    public static class Data implements Serializable {
        public int highScore = 0;
        public int totalKills = 0;
        public int gamesPlayed = 0;
        public boolean[] unlocked = {true, false, false, false};
        public int persistentCoins = 0;
    }

    public static Data load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE))) {
            return (Data) ois.readObject();
        } catch (Exception e) { return new Data(); }
    }

    public static void save(Data d) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE))) {
            oos.writeObject(d);
        } catch (Exception e) { e.printStackTrace(); }
    }
}