package kr.sniperpvp;

final class RifleMagazine {
    private final int capacity;
    private int rounds;

    RifleMagazine(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        this.capacity = capacity;
        this.rounds = capacity;
    }

    int capacity() {
        return capacity;
    }

    int rounds() {
        return rounds;
    }

    boolean consume() {
        if (rounds == 0) {
            return false;
        }
        rounds--;
        return true;
    }

    boolean isEmpty() {
        return rounds == 0;
    }

    boolean isFull() {
        return rounds == capacity;
    }

    void refill() {
        rounds = capacity;
    }
}
