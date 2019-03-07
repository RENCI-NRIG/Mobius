/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.openstack.nova.v2_0.domain;

 import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import org.jclouds.javax.annotation.Nullable;

 import javax.inject.Named;
import java.beans.ConstructorProperties;

 /**
 * Scheduler hints extension (alias "OS-SCH-HNT"): Hints passed directly to the scheduler via  /servers POST
 * 
 * @see <a href="http://developer.openstack.org/api-ref-compute-v2-ext.html#createServer" />
 *
 * Currently contains support for server group based filters only
 * <a href="http://docs.openstack.org/havana/config-reference/content/scheduler-filters.html#groupaffinityfilter"GroupAffinity</a>
 * and <a href="http://docs.openstack.org/havana/config-reference/content/scheduler-filters.html#groupantiaffinityfilter"GroupAntiAffinity</a>
 * Can be easily augmented to add support for other filters that can be configured via scheduler hints
*/
public class SchedulerHints {

    public static Builder builder() {
      return new ConcreteBuilder();
   }

    public Builder toBuilder() {
      return new ConcreteBuilder().fromSchedulerHints(this);
   }

    public static class Builder {

       protected String reservation;

       public Builder  reservation(String reservation) {
         this.reservation = reservation;
         return self();
      }

       public SchedulerHints build() {
         return new SchedulerHints(reservation);
      }

       public Builder  fromSchedulerHints(SchedulerHints in) {
         return this
               .reservation(in.getReservation());
      }

       protected Builder self(){
         return this;
      }
   }

    private static class ConcreteBuilder extends Builder {
      @Override
      protected ConcreteBuilder self() {
         return this;
      }
   }

    @Named("reservation")
   private final String reservation;

    @ConstructorProperties({"reservation"})
   protected SchedulerHints(@Nullable String reservation) {
      this.reservation = reservation;
   }

    @Nullable
   public String getReservation() {
      return this.reservation;
   }

    @Override
   public int hashCode() {
      return Objects.hashCode(reservation);
   }

    @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      SchedulerHints that = SchedulerHints.class.cast(obj);
      return Objects.equal(this.reservation, that.reservation);
   }

    protected ToStringHelper string() {
      return Objects.toStringHelper(this).add("reservation", reservation);
   }

    @Override
   public String toString() {
      return string().toString();
   }

 }
