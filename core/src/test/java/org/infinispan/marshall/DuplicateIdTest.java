package org.infinispan.marshall;

import org.infinispan.test.AbstractInfinispanTest;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Test(groups = "unit", testName = "marshall.DuplicateIdTest")
public class DuplicateIdTest extends AbstractInfinispanTest {
   public void testDuplicateMarshallerIds() throws Exception {
      Class idHolder = Ids.class;
      Map<Byte, Set<String>> dupes = new HashMap<Byte, Set<String>>();
      for (Field f : idHolder.getDeclaredFields()) {
         if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()) && f.getType().equals(byte.class)) {
            byte val = (Byte) f.get(null);
            Set<String> names = dupes.get(val);
            if (names == null) names = new HashSet<String>();
            names.add(f.getName());
            dupes.put(val, names);
         }
      }

      int largest = 0;
      for (Map.Entry<Byte, Set<String>> e : dupes.entrySet()) {
         assert e.getValue().size() == 1 : "ID " + e.getKey() + " is duplicated by fields " + e.getValue();
         largest = Math.max(largest, e.getKey());
      }

      System.out.println("Next available ID is " + (largest + 1));
   }
}
