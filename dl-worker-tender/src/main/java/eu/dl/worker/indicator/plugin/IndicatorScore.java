package eu.dl.worker.indicator.plugin;

/**
 * Holds score of indicator.
 */
public class IndicatorScore {
   private int total;
   private int hits;

   /**
    * Default constructor.
    */
   IndicatorScore() {
       this.total = 0;
       this.hits = 0;
   }

   /**
    * Updates score. Increments counter of tests and in case that {@code test} is true also increments
    * counter of success test.
    *
    * @param test
    *      test.
    */
   final void test(final boolean test) {
       total++;
       if (test) {
           hits++;
       }
   }

   /**
    * @return ratio as number of success tests divided by total number of tests or 0 for no tests.
    */
   final double ratio() {
       return total > 0 ? (double) hits / total * 100 : 0;
   }

    /**
     * @return total number of tests
     */
   final double getTotal() {
       return total;
   }
}