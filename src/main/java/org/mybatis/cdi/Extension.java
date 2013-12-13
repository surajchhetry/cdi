/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.cdi;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.inject.Inject;

/**
 * MyBatis CDI extension
 * 
 * @author Frank David Martínez
 */
public class Extension implements javax.enterprise.inject.spi.Extension {

  private final Logger logger = Logger.getLogger(getClass().getName());
  private final Set<MapperBean> mappers = new HashSet<MapperBean>();

  public <X> void processAnnotatedType(@Observes ProcessInjectionTarget<X> event, BeanManager beanManager) {
    final InjectionTarget<X> it = event.getInjectionTarget();
    for (final InjectionPoint ip : it.getInjectionPoints()) {
      if (ip.getAnnotated().isAnnotationPresent(Mapper.class)) {
        Annotation managerAnnotation = getManagerAnnotation(ip.getAnnotated().getAnnotations());
        mappers.add(new MapperBean((Class<?>) ip.getAnnotated().getBaseType(), managerAnnotation, beanManager));
      }
    }
  }
  
  private Annotation getManagerAnnotation(Set<Annotation> annotations) {
    Annotation managerAnnotation = null;
    for (Annotation annotation : annotations) {
      if (!annotation.annotationType().equals(Mapper.class) && !annotation.annotationType().equals(Inject.class)) {
        if (managerAnnotation != null) {
          throw new MybatisCdiConfigurationException("Cannot use more than one qualifier for a mapper");
        } else {
          managerAnnotation = annotation;
        }
      }
    }
    return managerAnnotation;
  }

  public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
    logger.log(Level.INFO, "MyBatis CDI Module - Activated");
    for (MapperBean mapperBean : mappers) {
      logger.log(Level.INFO, "MyBatis CDI Module - Mapper dependency discovered: {0}", mapperBean.getName());
      abd.addBean(mapperBean);
    }
    mappers.clear();
  }

}
