package ocp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Buckets {

    private static List<TimeAmountBucket> generateBuckets(int size) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        Random random = new Random();
        return IntStream.generate(() -> random.nextInt(1440))
                .limit(size)
                .collect(ArrayList::new, (l, i) -> l.add(new TimeAmountBucket(today.plusMinutes(i), BigDecimal.valueOf(random.nextInt(100)))), List::addAll);
    }

    public static void main(String[] args) {
        TreeSet<TimeAmountBucket> perMinuteItems = getItemsPerMinute();
        TreeSet<TimeAmountBucket> perHourItems = getItemsPerHour();
        System.out.println(perMinuteItems);
        System.out.println(perHourItems);
    }

    private static TreeSet<TimeAmountBucket> getItemsPerMinute() {
        final int minutesInDay = 1440;
        BigDecimal sod = BigDecimal.valueOf(100);

        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 0, 0);

        TreeMap<LocalDateTime, BigDecimal> map = generateBuckets(1000)
                .stream()
                .collect(Collectors.toMap(b -> b.time, b -> b.maxAmount, (t1, t2) -> t1, TreeMap::new));
        Stream.iterate(startOfDay, time -> time.plusMinutes(1))
                .limit(minutesInDay)
                .forEach(minute -> map.putIfAbsent(minute, BigDecimal.ZERO));

        BigDecimalHolder currSum = BigDecimalHolder.of(sod);
        ArrayList<BigDecimal> amounts = map.values().stream()
                .collect(() -> new ArrayList<BigDecimal>(minutesInDay), (list, nextV) -> list.add(currSum.add(nextV)), List::addAll);

        TreeSet<TimeAmountBucket> result = new TreeSet<>();
        int i = 0;
        for (LocalDateTime minute: map.keySet()) {
            result.add(new TimeAmountBucket(minute, amounts.get(i++)));
        }
        return result;
    }

    private static TreeSet<TimeAmountBucket> getItemsPerHour() {
        final int hoursInDay = 24;
        BigDecimal sod = BigDecimal.valueOf(100);

        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 0, 0);

        TreeMap<LocalDateTime, TreeSet<TimeAmountBucket>> hourGroups = generateBuckets(1000)
                .stream()
                .collect(Collectors.groupingBy(b -> b.time.withMinute(0), TreeMap::new, Collectors.toCollection(TreeSet::new)));
        TreeSet<TimeAmountBucket> result = new TreeSet<>();
        hourGroups.forEach((hour, set) -> result.add(new TimeAmountBucket(hour, maxPeriodFrom(set), minPeriodFrom(set))));
        Stream.iterate(startOfDay, time -> time.plusHours(1))
                .limit(hoursInDay)
                .forEach(hour -> result.add(new TimeAmountBucket(hour, BigDecimal.ZERO)));
        BigDecimalHolder currMin = BigDecimalHolder.of(sod);
        BigDecimalHolder currMax = BigDecimalHolder.of(sod);

        result.forEach(bucket -> {
            bucket.minAmount = currMin.add(bucket.minAmount);
            bucket.maxAmount = currMax.add(bucket.maxAmount);
        });

        return result;
    }

    private static BigDecimal minPeriodFrom(TreeSet<TimeAmountBucket> hourRange) {
        if (hourRange.size() == 1) {
            return hourRange.first().minAmount;
        }
        if (hourRange.size() == 2) {
            return hourRange.first().minAmount.min(hourRange.last().minAmount);
        }

        Iterator<TimeAmountBucket> iterator = hourRange.iterator();
        BigDecimal min = iterator.next().minAmount;
        BigDecimal currAmount = min;

        while (iterator.hasNext()) {
            currAmount = currAmount.add(iterator.next().minAmount);
            min = currAmount.min(min);
        }

        return min;
    }

    private static BigDecimal maxPeriodFrom(TreeSet<TimeAmountBucket> hourRange) {
        if (hourRange.size() == 1) {
            return hourRange.first().minAmount;
        }
        if (hourRange.size() == 2) {
            return hourRange.first().minAmount.max(hourRange.last().minAmount);
        }

        Iterator<TimeAmountBucket> iterator = hourRange.iterator();
        BigDecimal max = iterator.next().minAmount;
        BigDecimal currAmount = max;

        while (iterator.hasNext()) {
            currAmount = currAmount.add(iterator.next().minAmount);
            max = currAmount.max(max);
        }

        return max;
    }

    private static class BigDecimalHolder {
        private BigDecimal value;

        private static BigDecimalHolder of(BigDecimal value) {
            BigDecimalHolder holder = new BigDecimalHolder();
            holder.value = Objects.requireNonNull(value);
            return holder;
        }

        private BigDecimal add(BigDecimal toAdd) {
            value = value.add(toAdd);
            return value;
        }
    }

    private static class TimeAmountBucket implements Comparable<TimeAmountBucket> {
        private LocalDateTime time;
        private BigDecimal maxAmount;
        private BigDecimal minAmount;

        private TimeAmountBucket(LocalDateTime time, BigDecimal maxAmount) {
            this.time = Objects.requireNonNull(time);
            this.maxAmount = Objects.requireNonNull(maxAmount);
            this.minAmount = maxAmount;
        }

        private TimeAmountBucket(LocalDateTime time, BigDecimal maxAmount, BigDecimal minAmount) {
            this.time = Objects.requireNonNull(time);
            this.maxAmount = Objects.requireNonNull(maxAmount);
            this.minAmount = Objects.requireNonNull(minAmount);
        }

        @Override
        public int compareTo(TimeAmountBucket o) {
            return time.compareTo(o.time);
        }

        @Override
        public int hashCode() {
            return time.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return ((o instanceof TimeAmountBucket) && time.equals(((TimeAmountBucket) o).time));
        }

        @Override
        public String toString() {
            return time.toString() + ". maxAmount: " + maxAmount + ", minAmount: " + minAmount;
        }
    }
}
