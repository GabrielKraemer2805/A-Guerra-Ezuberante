package io.github.omarflk.guerraezuberante.audio;

import java.util.*;

/** Each item once per round; no repeat across rounds when alternatives exist. */
public final class ShuffleBag<T> {
    private final List<T> library;
    private final List<T> pending = new ArrayList<>();
    private final Random random;
    private T last;
    public ShuffleBag(List<T> library, long seed) { this.library = List.copyOf(new LinkedHashSet<>(library)); random = new Random(seed); }
    public Optional<T> next() {
        if (library.isEmpty()) return Optional.empty();
        if (pending.isEmpty()) {
            pending.addAll(library); Collections.shuffle(pending, random);
            if (pending.size() > 1 && Objects.equals(pending.get(0), last)) Collections.swap(pending, 0, 1);
        }
        last = pending.remove(0); return Optional.of(last);
    }
}
