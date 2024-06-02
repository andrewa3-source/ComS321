/* To compile this program:
 *   gcc cs321_pa1_f2023_cycle_sort.c -o cs321_pa1_f2023_cycle_sort
 * Change N and recompile to run on arrays of different sizes.
 *
 * For this assignment, you'll be implementing cycle sort in the LEG subset 
 * of the ARMv8 ISA.  Cycle sort is an n-squared comparison sort which has 
 * the interesting feature that it does the theoretically optimal number of 
 * moves in the array being sorted.
 *
 * Cycle sort is a bit of an obscure sort, which sees little to no practicle
 * use, but which is theoretically interesting for its relationship with the
 * algebra of groups.  I want to stess here that you do not have to understand
 * this algorithm!  You don't even have to try to understand it.  All that you
 * have to do is, essentially, be the compiler and translate this C code into
 * LEGv8.  That said, if you'd like to understand the algorithm, you can learn
 * more about it here: https://en.wikipedia.org/wiki/Cycle_sort.  Additionally,
 * https://www.youtube.com/watch?v=jTN7vLqzigc goes through it by hand in the
 * first few minutes (I don't recommend the entire video, just the bit with
 * the toy cars at the beginning).  Lastly, another problem that uses cycles
 * in groups in its solution is the 100 prisoners problem.  Cycle sort and the
 * counter-intuitive solution to the 100 prisoners problem work for the same
 * reason, so if you understand one, you'll likely understand the other.  Here
 * is a fun video presenting the 100 prisoners problem:
 * https://www.youtube.com/watch?v=a1DUUnhk3uE
 *
 * See the comments and code below for details on exactly what you must 
 * implement.
 */

#define N 10000

/* Here is a single-function version of cycle sort.  This, or something very
 * similar, is probably what you would write if you were doing this in a 
 * higher-level language.
 *
 * Below, we break this into a number of smaller functions.  We do this for
 * two reasons:
 *
 *   1) The bigger a procedure is, the more difficult it (usually) is to 
 *      implement it in assembly (in particular, the refactor eliminates
 *      nested loops); and
 *   2) We want to force you to implement multiple procedures, use the stack,
 *      and adhere to ARMv8 calling conventions.
 *
 * You will NOT be implementing this function.  You'll be implementing the
 * broken-down version of it below.  This version is only here for reference.
 */
void cycle_sort_one_function_do_not_implement(int a[], int n)
{
  int i;
  int cstart;
  int val, idx;
  int tmp;

  /* The outer loops locates new cycles in the input */
  for (cstart = 0; cstart < n - 1; cstart++) {
    val = a[cstart];
    idx = cstart;

    /* Search for the correct index for val (count items smaller than it) */
    for (i = cstart + 1; i < n; i++) {
      if (a[i] < val) {
        idx++;
      }
    }

    /* Check for a self-cycle */
    if (idx == cstart) {
      continue;
    }

    /* Skip duplicate entries */
    while (val == a[idx]) {
      idx++;
    }
    /* And insert */
    tmp = val;
    val = a[idx];
    a[idx] = tmp;

    /* After swapping, we've got a new value at a new index.  We *
     * need to complete the cycle (see the video with the cars). */
    while (idx != cstart) {
      /* Notice that the body of this while loop is the same as above,  *
       * except that we don't check for self-cycles (since those aren't *
       * possible here).                                                */
      idx = cstart;

      for (i = cstart + 1; i < n; i++) {
        if (a[i] < val) {
          idx++;
        }
      }

      while (val == a[idx]) {
        idx++;
      }
      tmp = val;
      val = a[idx];
      a[idx] = tmp;
    }
  }
}

/* swap swaps the value at address p with the value at address q. *
 *                                                                *
 * A note in C syntax: Astericks in declarations denote that the  *
 * variables are pointers (that is, they hold address; int *a is  *
 * a variable that holds the address of an int).  Astericks in    *
 * expressions dereference pointers (they get the value stored at *
 * the address; thus, *a is the integer located at the address    *
 * stored in a).                                                  */
void swap(int *a, int *b)
{
  int tmp;

  tmp = *a;
  *a = *b;
  *b = tmp;
}

/* find_index returns the sorted-order index of value in a. *
 * Pre-condition: Items in a at indices less than start are *
 * already in sorted position.                              *
 *                                                          *
 * A note on C semantics: Arrays and pointers are           *
 * functionally almost identical.  Thus, just as a and b in *
 * swap are addresses, here a contains an address.          */
int find_index(int a[], int n, int start, int value)
{
  int i;
  int index;

  index = start;
  for (i = start + 1; i < n; i++) {
    if (a[i] < value) {
      index++;
    }
  }

  return index;
}

/* skip_duplicates returns the index of the first element in a *
 * after start which is not equal to value.                    */
int skip_duplicates(int a[], int start, int value)
{
  int index;
  
  for (index = start; value == a[index]; index++)
    ;

  return index;
}

/* Complete cycle finds the index and does the swap for each element *
 * in the cycle beginning at start.                                  *
 *                                                                   *
 * Note that this implements the "while (idx != cstart) {" loop from *
 * the reference implementation at the top of this file.             */
void complete_cycle(int a[], int n, int start, int index, int value)
{
  while (index != start) {
    index = find_index(a, n, start, value);

    index = skip_duplicates(a, index, value);

    swap(&value, &a[index]);
  }
}

/* cycle_sort sorts the array a (of n elements) using the cycle sort *
 * algorithm in the refactored form using the helper function given  *
 * above.                                                            */
void cycle_sort(int a[], int n)
{
  int cstart;
  int val, idx;

  for (cstart = 0; cstart < n - 1; cstart++) {
    val = a[cstart];

    idx = find_index(a, n, cstart, val);

    if (idx == cstart) {
      continue;
    }

    idx = skip_duplicates(a, idx, val);

    swap(&val, &a[idx]);

    complete_cycle(a, n, cstart, idx, val);
  }
}

/* fill fills the array a (of n elements) with decreasing values from *
 * n - 1 to zero (reverse sorted order).                              */
void fill(int a[], int n) {
  int i;
  
  for (i = 0; i < n; i++) {
    a[i] = n - i - 1;
  }
}

/* binary_search is an implementation of the standard recursive *
 * binary search algorithm.  It searches the array a between    *
 * the values of start (inclusive) and end (exclusive) for      *
 * value.  If value is found, its index is returned; otherwise  *
 * the function returns -1 to indicate failure.                 */
int binary_search(int *a, int start, int end, int value)
{
  int index;

  if (end < start) {
    return -1;
  }

  index = (start + end) / 2;

  if (a[index] == value) {
    return index;
  }
  if (a[index] > value) {
    return binary_search(a, start, index - 1, value);
  }
  return binary_search(a, index + 1, end, value);
}

/* Your main function should allocate space for an array, call fill to   *
 * fill it with decreasing numbers, and then call cycle_sort to sort     *
 * it.  Use the HALT emulator instruction to see the memory contents and *
 * confirm that your functions work.  You may choose any array size you  *
 * like (up to the default limit of memory, 4096 bytes or 512 8-byte     *
 * integers).                                                            *
 *                                                                       * 
 * After sorting, call binary search with 4 values: the smallest,        *
 * largest, and middle items in your array, followed by a value not in   *
 * the array.  After each call PRNT X0 to display the return value.      *
 *                                                                       *
 * After completing all of the above, HALT the emulator to force a core  *
 * dump so that you (and the TAs) can examing the contents of memory.    *
 *                                                                       *
 * You must implement all functions described above except for the       *
 * reference implementation of cycle sort.  You are acting as the        *
 * compiler here; you are not granted creative freedom to refactor these *
 * functions to your liking.  You must adhere to ARMv8 calling           *
 * conventions; in particular, you must correctly use the stack when     *
 * calling and implementing procedures, and a procedure may not "know"   *
 * anything that it was not explicitly informed of by way of its         *
 * parameters!  For example: Even though you--the programmer--know that  *
 * no other procedure uses X22, you still must save it before you use it *
 * and restore it when you are finished with it.  Imagine that, instead  *
 * of you writing all of these procedures, each of them is written by a  *
 * different person, but none of you are permitted to communicate with   *
 * each other in any way.  The only thing each of you has is this        *
 * specification.  When we put all of your procedures together, your     *
 * program should work, but the only way that will be possible is if you *
 * fully adhere to convention.                                           *
 *                                                                       *
 * You may work alone or with up to 1 partner on this assignment.        */
int main(int argc, char *argv[])
{
  int a[N];

  fill(a, N);

  cycle_sort(a, N);

  /* Returns 0 */
  binary_search(a, 0, N - 1, 0);
  /* Returns 9999 */
  binary_search(a, 0, N - 1, N - 1);
  /* Returns 5000 */
  binary_search(a, 0, N - 1, N / 2);
  /* Returns -1 */
  binary_search(a, 0, N - 1, N);
  
  return 0;
}
