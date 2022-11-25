import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClientCodes {
    ArrayList<String> codes = new ArrayList<>();

    public String newCode(){
        boolean added;
        String code;
        do {
            code = generateCode();
            added = addToCodes(code);
        } while (!added);
        return code;
    }

    public String generateCode(){
        Random r = new Random();
        char A = (char)(r.nextInt(26) + 'a');
        char B = (char)(r.nextInt(26) + 'a');
        String C = Integer.toString(ThreadLocalRandom.current().nextInt(0, 9 + 1));
        String code = A+B+C;
        return code;
    }

    public boolean isOnCodes(String code) {
        return this.filter(c -> c.equals(code), this.codes).size() > 0;
    }

    public<T> List<T> filter(Predicate<T> criteria, ArrayList<T> list) {
        return list.stream().filter(criteria).collect(Collectors.<T>toList());
    }

    private boolean addToCodes(String code) {
        try {
            if(this.isOnCodes(code)) return false;
            this.codes.add(code);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
