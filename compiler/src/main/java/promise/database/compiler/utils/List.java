/*
 * Copyright 2017, Peter Vincent
 * Licensed under the Apache License, Version 2.0, Android Promise.
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package promise.database.compiler.utils;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import kotlin.Pair;
import promise.database.compiler.utils.function.BIConsumer;
import promise.database.compiler.utils.function.Combiner;
import promise.database.compiler.utils.function.FilterFunction;
import promise.database.compiler.utils.function.FilterFunction2;
import promise.database.compiler.utils.function.GroupFunction;
import promise.database.compiler.utils.function.GroupFunction2;
import promise.database.compiler.utils.function.BIConsumer;
import promise.database.compiler.utils.function.Combiner;
import promise.database.compiler.utils.function.FilterFunction;
import promise.database.compiler.utils.function.FilterFunction2;
import promise.database.compiler.utils.function.GroupFunction;
import promise.database.compiler.utils.function.GroupFunction2;
import promise.database.compiler.utils.function.GroupFunction3;
import promise.database.compiler.utils.function.JoinFunction;
import promise.database.compiler.utils.function.MapFunction;
import promise.database.compiler.utils.function.MapIndexFunction;
import promise.database.compiler.utils.function.ReduceFunction;

public class List<T> extends ArrayList<T> {
  /**
   * Constructs an empty list with the specified initial capacity.
   *
   * @param initialCapacity the initial capacity of the list
   * @throws IllegalArgumentException if the specified initial capacity is negative
   */
  public List(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructs an empty list with an initial capacity of ten.
   */
  public List() {
  }

  /**
   * Constructs a list containing the elements of the specified collection, in the order they are
   * returned by the collection's iterator.
   *
   * @param c the collection whose elements are to be placed into this list
   * @throws NullPointerException if the specified collection is null
   */
  public List(Collection<? extends T> c) {
    super(c);
  }

  /**
   * returns a list from an array of args
   *
   * @param t   array of args
   * @param <T> type of list elements
   * @return a list
   */
  @SafeVarargs
  public static <T> List<T> fromArray(T... t) {
    return new List<>(Arrays.asList(t));
  }

  public static <T> List<T> generate(int num, MapFunction<? extends T, ? super Integer> function) {
    List<T> list = new List<>();
    for (int i = 0; i < num; i++) list.add(function.from(i));
    return list;
  }

  /**
   * reverses this list
   *
   * @return a reversed list
   */
  public List<T> reverse() {
    Collections.reverse(this);
    return this;
  }

  /**
   * gets a sample elements from the list
   *
   * @param size elements to take
   *             {@link java.util.List#subList(int, int)}
   *             where fromIndex is zero
   * @return a sublist
   */
  public List<T> take(int size) {
    List<T> list = new List<>();
    if (size() > size) for (int i = 0; i < size; i++) list.add(get(i));
    else list = this;
    return list;
  }

  /**
   * finds an item within the list
   *
   * @param function select function
   * @return an item or null if not found
   */
  @Nullable
  public T find(FilterFunction<? super T> function) {
    return filter(function).first();
  }

  /**
   * find the index where the condition in the select function is true
   * returns -1 if item is not found
   *
   * @param function select function
   * @return index of item marching the condition
   */
  public int findIndex(FilterFunction<? super T> function) {
    int match = 0;
    for (T t : this) {
      if (function.select(t)) return match;
      match++;
    }
    return -1;
  }

  /**
   * shiffle the elements of the list
   *
   * @return
   */
  public List<T> shuffled() {
    Collections.shuffle(this);
    return this;
  }

  /**
   * return a transformation of all the items
   *
   * @param function transformer function
   * @param <E>      desires transformation result
   * @return transformed list
   */
  public <E> List<E> map(MapFunction<? extends E, ? super T> function) {
    List<E> list = new List<>();
    for (T t : this) list.add(function.from(t));
    return list;
  }

  public <E> List<E> mapIndexed(MapIndexFunction<? extends E, ? super T> function) {
    List<E> list = new List<>();
    for (int i = 0; i < this.size(); i++) {
      T t = get(i);
      list.add(function.from(i, t));
    }
    return list;
  }
/*
    public <K, V> Map<K, V> associate(MapFunction<Pair<K, V>, ? super T> function) {

    }

    public List<List<T>> chucked(int chuckSize) {

    }

    public <R> List<R> chucked(int chuckSize, MapFunction<List<? extends T>, ? super R> function) {

    }*/

  public Pair<List<? extends T>, List<? extends T>> partition(FilterFunction<? super T> function) {
    List<T> first = new List<>();
    List<T> second = new List<>();
    for (T t : this) {
      if (function.select(t)) first.add(t);
      else second.add(t);
    }
    return new Pair(first, second);
  }

  /**
   * sorts the elements of the list
   *
   * @param comparator compare function
   * @return sorted list
   */
  public List<T> sorted(Comparator<? super T> comparator) {
    Collections.sort(this, comparator);
    return this;
  }

  /**
   * arrange the list based on an atrribute that is not in blueprint of original items
   *
   * @param function   transformer for list to desired attribute that is sortable
   * @param comparator compare fucntion that compares the result of the transformer
   * @param <K>        desired compare type
   * @return a sorted list
   */
  public <K> List<T> arranged(final MapFunction<? extends K, ? super T> function, final Comparator<? super K> comparator) {
    return map(t -> new Arrangeable<T, K>().value(t).key(function.from(t)))
        .sorted(
            (o1, o2) -> comparator.compare(o1.key(), o2.key()))
        .map(Arrangeable::value);
  }

  /**
   * groups the items into collections with similar attributes
   *
   * @param function group function
   * @param <E>      type that will contain one group of similar items
   * @return list of collected groups
   */
  public <E> List<E> group(GroupFunction3<E, T> function) {
    return function.group(this);
  }

  /**
   * groups the items into groups with similar attributes
   *
   * @param function group function
   * @param <K>      type of similar attribute
   * @param <E>      collected group type
   * @return a list of collected group type
   */
  public <K, E> List<E> group(GroupFunction2<K, E, T> function) {
    List<E> es = new List<>();
    Map<K, List<T>> map = new LinkedHashMap<>();
    for (int i = 0, size = this.size(); i < size; i++) {
      T t = this.get(i);
      K key = function.getKey(t);
      if (map.containsKey(key)) {
        List<T> list = map.get(key);
        list.add(t);
      } else {
        List<T> list = new List<>();
        list.add(t);
        map.put(key, list);
      }
    }
    for (Map.Entry<K, List<T>> entry : map.entrySet()) {
      E e = function.get(entry.getKey());
      function.apply(e, entry.getValue());
      es.add(e);
    }
    return es;
  }

  /**
   * groups a list of items with similar attribute
   *
   * @param function group function
   * @param <K>      similar attribute type
   * @return a list of category of items
   * each category returned has a list of items with their similar attribute
   * {@link Category#list() contains grouped items}
   * {@link Category#name()} contains the name of group that is the same as the value of the similar attribute
   */
  public <K> List<Category<K, T>> groupBy(final GroupFunction<? extends K, ? super T> function) {
    return group(
        new GroupFunction2<K, Category<K, T>, T>() {
          @Override
          public K getKey(T t) {
            return function.getKey(t);
          }

          @Override
          public Category<K, T> get(K k) {
            return new Category<>(k);
          }

          @Override
          public void apply(Category<K, T> category, List<T> list) {
            category.list(list);
          }
        });
  }

  /**
   * reduces this list to only contain the elements with similarity to the elements in the
   * provided list
   *
   * @param uList    compare list
   * @param function compare function that compares each item in this list to each item
   *                 in the provided uList
   * @param <U>      type of item in uList
   * @return a small list with elements similar to the elements in uList
   */
  public <U> List<T> joinOn(List<? extends U> uList, JoinFunction<? super T, ? super U> function) {
    List<T> ts = new List<>();
    for (int i = 0, tSize = this.size(); i < tSize; i++) {
      T t = this.get(i);
      for (int i1 = 0, uSize = uList.size(); i1 < uSize; i1++) {
        U u = uList.get(i1);
        if (function.joinBy(t, u)) {
          ts.add(t);
          break;
        }
      }
    }
    return ts;
  }

  public <U> List<T> join(List<? extends U> uList, Combiner<? super U, T> function) {
    if (this.size() != uList.size())
      throw new IllegalArgumentException("Samples must be of same size");
    List<T> ts = new List<>();
    for (int i = 0, size = this.size(); i < size; i++) {
      T t = this.get(i);
      U u = uList.get(i);
      ts.add(function.join(t, u));
    }
    return ts;
  }

  /**
   * returns a list with no duplicate elements
   *
   * @return
   */
  public List<T> uniques() {
    return new List<>(new HashSet<>(this));
  }

  /**
   * returns a list with unique items based on a given attribute in each item in the list
   *
   * @param function     maps to the items that is supposed to be unique
   * @param joinFunction reduces the this list with the result
   *                     of unique list of attribute that is supposed to be unique
   * @param <K>          type of desired unique attribute
   * @return a unique list if items
   */
  public <K> List<T> uniques(MapFunction<? extends K, ? super T> function, JoinFunction<? super T, ? super K> joinFunction) {
    return joinOn(map(function).uniques(), joinFunction);
  }

  /**
   * reduces this list with comparing to the elements of another list
   *
   * @param uList    list to compare from
   * @param function filter function for each item
   * @param equals  flags to compare equality or no equality
   * @param <U>      type of list item to compare from
   * @param <K>      type of attribute to compare
   * @return a reduced list
   */
  public <U, K> List<T> reduce(
      List<? extends U> uList, final FilterFunction2<? extends K, ? super U, ? super T> function, final boolean equals) {
    final Set<K> set = new HashSet<>(uList.map(
        function::getKey));
    return filter(
        t -> {
          if (equals) return set.contains(function.filterBy(t));
          else return !set.contains(function.filterBy(t));
        });
  }

  /**
   * @param list
   * @param reverse
   * @param function
   * @param combiner
   * @param <K>
   * @param <U>
   * @return
   */
  public <K, U> List<T> merge(
      List<? extends U> list,
      final boolean reverse,
      final FilterFunction2<? extends K, ? super U, ? super T> function,
      Combiner<? super U, T> combiner) {
    final Set<K> set = new HashSet<>(list.map(
        function::getKey));
    return filter(
        t -> reverse == set.contains(function.filterBy(t)))
        .join(list, combiner);
  }

  /**
   * @param list
   * @param function
   * @param <K>
   * @param <MERGE>
   * @return
   */
  public <K, MERGE> List<Pair<T, K>> mergeWith(List<? extends K> list, final FilterFunction2<? extends MERGE, ? super K, ? super T> function) {
    List<Pair<T, K>> list1 = new List<>();
    /*Set<Arrangeable<K, MERGE>> set = new HashSet<>(
        list.map(new MapFunction<Arrangeable<K, MERGE>, K>() {
          @Override
          public Arrangeable<K, MERGE> from(K k) {
            return new Arrangeable<K, MERGE>().key(function.getKey(k)).value(k);
          }
        })
    );*/
    for (T t : this)
      for (K k : list)
        if (function.getKey(k).equals(function.filterBy(t))) list1.add(new Pair<>(t, k));
    return list1;
  }

  /**
   * returns whether there's an item in the list that matches the condition flagged in the function
   *
   * @param function flag function
   * @return if there's an item matching in the list
   */
  public boolean anyMatch(FilterFunction<? super T> function) {
    boolean match = false;
    for (T t : this)
      if (function.select(t)) {
        match = true;
        break;
      }
    return match;
  }

  /**
   * returns whether all the items in this list match the condition in the flag function
   *
   * @param function flag function
   * @return if all items match the flag condition
   */
  public boolean allMatch(FilterFunction<? super T> function) {
    boolean match = true;
    for (T t : this)
      match = match && function.select(t);
    return match;
  }

  /**
   * filters this list to only items matching the condition
   *
   * @param function filter function
   * @return a reduces list
   */
  public List<T> filter(FilterFunction<? super T> function) {
    List<T> list = new List<>();
    for (T t : this) if (function.select(t)) list.add(t);
    return list;
  }

  /**
   * @param list
   * @param function
   * @param <U>
   * @return
   */
  public <U> List<T> reduce(List<? extends U> list, JoinFunction<? super T, ? super U> function) {
    List<T> ts = new List<>();
    for (int i = 0; i < this.size(); i++) {
      T t = this.get(i);
      for (int j = 0; j < list.size(); j++)
        if (!function.joinBy(t, list.get(j))) {
          ts.add(t);
          break;
        }
    }
    return ts;
  }

  /**
   * @param consumer
   */
  public void biConsume(BIConsumer<? super T, ? super T> consumer) {
    Iterator<T> it = iterator();
    if (!it.hasNext()) return;
    T first = it.next();
    while (it.hasNext()) {
      T next = it.next();
      consumer.accept(first, next);
      first = next;
    }
  }

  /**
   * gets the first element in the list
   *
   * @return first element
   */
  @Nullable
  public T first() {
    return this.isEmpty() ? null : this.get(0);
  }

  /**
   * gets the last element in the list
   *
   * @return last element
   */
  @Nullable
  public T last() {
    return this.isEmpty() ? null : get(size() - 1);
  }

  /**
   * reduces all the items in the list to one result
   *
   * @param function reducing function
   * @param <K>      desired type of reduce result
   * @return an instance of reduce result
   */
  public <K> K reduce(ReduceFunction<? extends K, T> function) {
    return function.reduce(this);
  }
}

