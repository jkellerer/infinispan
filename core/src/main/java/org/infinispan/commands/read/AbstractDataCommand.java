/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.commands.read;

import org.infinispan.commands.DataCommand;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;

import java.util.Set;

/**
 * @author Mircea.Markus@jboss.com
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @since 4.0
 */
public abstract class AbstractDataCommand implements DataCommand, FlagAffectedCommand {
   protected Object key;
   protected Set<Flag> flags;

   public Object getKey() {
      return key;
   }

   public void setKey(Object key) {
      this.key = key;
   }
   
   public Set<Flag> getFlags() {
      return flags;
   }
   
   public void setFlags(Set<Flag> flags) {
      this.flags = flags;
   }

   protected AbstractDataCommand(Object key, Set<Flag> flags) {
      this.key = key;
      this.flags = flags;
   }

   protected AbstractDataCommand() {
   }

   public abstract void setParameters(int commandId, Object[] parameters);

   public abstract Object[] getParameters();

   public boolean shouldInvoke(InvocationContext ctx) {
      return true;
   }
   
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AbstractDataCommand other = (AbstractDataCommand) obj;
      if (key == null) {
         if (other.key != null)
            return false;
      } else if (!key.equals(other.key))
         return false;
      if (flags == null) {
         if (other.flags != null)
            return false;
      } else if (!flags.equals(other.flags))
         return false;
      return true;
   }
   
   @Override
   public int hashCode() {
      return (key != null ? key.hashCode() : 0);
   }
   
   @Override
   public String toString() {
      return new StringBuilder(getClass().getSimpleName())
         .append(" {key=")
         .append(key)
         .append(", flags=").append(flags)
         .append("}")
         .toString();
   }

}
