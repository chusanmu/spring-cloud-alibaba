/*
 * Copyright 2013-2018 the original author or authors.
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

package com.alibaba.cloud.seata.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * seata 拦截器，用于传递xid， 这里使用的是spring拦截器
 * @author xiaojing
 *
 * Seata HandlerInterceptor, Convert Seata information into
 * @see io.seata.core.context.RootContext from http request's header in
 * {@link org.springframework.web.servlet.HandlerInterceptor#preHandle(HttpServletRequest , HttpServletResponse , Object )},
 * And clean up Seata information after servlet method invocation in
 * {@link org.springframework.web.servlet.HandlerInterceptor#afterCompletion(HttpServletRequest, HttpServletResponse, Object, Exception)}
 */
public class SeataHandlerInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory
			.getLogger(SeataHandlerInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) {

		String xid = RootContext.getXID();
		// TODO: 从request头中，把xid取到
		String rpcXid = request.getHeader(RootContext.KEY_XID);
		if (log.isDebugEnabled()) {
			log.debug("xid in RootContext {} xid in RpcContext {}", xid, rpcXid);
		}
		// TODO: 如果xid 在当前请求中不存在 ok，那就去bind上 当前的threadLocal
		if (xid == null && rpcXid != null) {
			// TODO: 绑定rpcXid
			RootContext.bind(rpcXid);
			if (log.isDebugEnabled()) {
				log.debug("bind {} to RootContext", rpcXid);
			}
		}
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception e) {
		// TODO: 完成这次请求之后，从请求头中把xid取到
		String rpcXid = request.getHeader(RootContext.KEY_XID);

		if (StringUtils.isEmpty(rpcXid)) {
			return;
		}
		// TODO: 进行解绑
		String unbindXid = RootContext.unbind();
		if (log.isDebugEnabled()) {
			log.debug("unbind {} from RootContext", unbindXid);
		}
		// TODO: 如果解绑的和当前的不是一个
		if (!rpcXid.equalsIgnoreCase(unbindXid)) {
			log.warn("xid in change during RPC from {} to {}", rpcXid, unbindXid);
			if (unbindXid != null) {
				// TODO: 又重新绑定上了
				RootContext.bind(unbindXid);
				log.warn("bind {} back to RootContext", unbindXid);
			}
		}
	}

}
