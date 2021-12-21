/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		//调用BeanDefinitionRegistryPostProcessor的后置处理器 Begin
		// 定义已处理的后置处理器
		Set<String> processedBeans = new HashSet<>();

		//判断我们的beanFactory实现了BeanDefinitionRegistry(实现了该结构就有注册和获取Bean定义的能力）
		if (beanFactory instanceof BeanDefinitionRegistry) {
			//强行把我们的bean工厂转为BeanDefinitionRegistry，因为待会需要注册Bean定义
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			//保存BeanFactoryPostProcessor类型的后置   BeanFactoryPostProcessor 提供修改
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			//保存BeanDefinitionRegistryPostProcessor类型的后置处理器 BeanDefinitionRegistryPostProcessor 提供注册
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			//循环我们传递进来的beanFactoryPostProcessors
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				//判断我们的后置处理器是不是BeanDefinitionRegistryPostProcessor
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					//进行强制转化
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					//调用他作为BeanDefinitionRegistryPostProcessor的处理器的后置方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					//添加到我们用于保存的BeanDefinitionRegistryPostProcessor的集合中
					registryProcessors.add(registryProcessor);
				}
				else {
					//若没有实现BeanDefinitionRegistryPostProcessor 接口，那么他就是BeanFactoryPostProcessor
					//把当前的后置处理器加入到regularPostProcessors中
					regularPostProcessors.add(postProcessor);
				}
			}

			//定义一个集合用户保存当前准备创建的BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			//第一步:去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//循环筛选出来的匹配BeanDefinitionRegistryPostProcessor的类型名称
			for (String ppName : postProcessorNames) {
				//判断是否实现了PriorityOrdered接口的
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//同时也加入到processedBeans集合中去
					processedBeans.add(ppName);
				}
			}
			//对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 把当前的加入到总的里面去
			registryProcessors.addAll(currentRegistryProcessors);
			/**
			 * 在这里典型的BeanDefinitionRegistryPostProcessor就是ConfigurationClassPostProcessor
			 * 用于进行bean定义的加载 比如我们的包扫描，@import  等等。。。。。。。。。
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			//调用完之后，马上clea掉
			currentRegistryProcessors.clear();

			//---------------------------------------调用内置实现PriorityOrdered接口ConfigurationClassPostProcessor完毕--优先级No1-End----------------------------------------------------------------------------------------------------------------------------
			//去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称（内置的和上面注册的）
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//循环上一步获取的BeanDefinitionRegistryPostProcessor的类型名称
			for (String ppName : postProcessorNames) {
				//表示没有被处理过,且实现了Ordered接口的
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					//显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//同时也加入到processedBeans集合中去
					processedBeans.add(ppName);
				}
			}
			//对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//把他加入到用于保存到registryProcessors中
			registryProcessors.addAll(currentRegistryProcessors);
			//调用他的后置处理方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			//调用完之后，马上clea掉
			currentRegistryProcessors.clear();

//-----------------------------------------调用自定义Order接口BeanDefinitionRegistryPostProcessor完毕-优先级No2-End-----------------------------------------------------------------------------------------------------------------------------
			//调用没有实现任何优先级接口的BeanDefinitionRegistryPostProcessor
			//定义一个重复处理的开关变量 默认值为true
			boolean reiterate = true;
			//第一次就可以进来
			while (reiterate) {
				//进入循环马上把开关变量给改为false
				reiterate = false;
				//去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				//循环上一步获取的BeanDefinitionRegistryPostProcessor的类型名称
				for (String ppName : postProcessorNames) {
					//没有被处理过的
					if (!processedBeans.contains(ppName)) {
						//显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						//同时也加入到processedBeans集合中去
						processedBeans.add(ppName);
						//再次设置为true
						reiterate = true;
					}
				}
				//对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				//把他加入到用于保存到registryProcessors中
				registryProcessors.addAll(currentRegistryProcessors);
				//调用他的后置处理方法
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				//进行clear
				currentRegistryProcessors.clear();
			}

//-----------------------------------------调用没有实现任何优先级接口自定义BeanDefinitionRegistryPostProcessor完毕--End-----------------------------------------------------------------------------------------------------------------------------
			//调用 BeanDefinitionRegistryPostProcessor.postProcessBeanFactory方法
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			//调用BeanFactoryPostProcessor 自设的（没有）
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			//若当前的beanFactory没有实现了BeanDefinitionRegistry 说明没有注册Bean定义的能力
			// 那么就直接调用BeanDefinitionRegistryPostProcessor.postProcessBeanFactory方法
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

//-----------------------------------------所有BeanDefinitionRegistryPostProcessor调用完毕--End-----------------------------------------------------------------------------------------------------------------------------


//-----------------------------------------处理BeanFactoryPostProcessor --Begin-----------------------------------------------------------------------------------------------------------------------------


		//不要在这里初始化FactoryBeans:我们需要保留所有常规bean
		//未初始化，让bean工厂的后处理程序应用于它们!
		//获取容器中所有的 BeanFactoryPostProcessor
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		//保存BeanFactoryPostProcessor类型实现了priorityOrdered
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		//保存BeanFactoryPostProcessor类型实现了Ordered接口的
		List<String> orderedPostProcessorNames = new ArrayList<>();
		//保存BeanFactoryPostProcessor没有实现任何优先级接口的
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			//processedBeans包含的话，表示在上面处理BeanDefinitionRegistryPostProcessor的时候处理过了
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			//判断是否实现了PriorityOrdered 优先级最高
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			//判断是否实现了Ordered  优先级 其次
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			//没有实现任何的优先级接口的  最后调用
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		//排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 先调用BeanFactoryPostProcessor实现了 PriorityOrdered接口的
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		//再调用BeanFactoryPostProcessor实现了 Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		//调用没有实现任何方法接口的
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
//-----------------------------------------处理BeanFactoryPostProcessor --End-----------------------------------------------------------------------------------------------------------------------------

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();

//------------------------- BeanFactoryPostProcessor和BeanDefinitionRegistryPostProcessor调用完毕 --End-----------------------------------------------------------------------------------------------------------------------------

	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanFactory(beanFactory);
			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		}
		else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
