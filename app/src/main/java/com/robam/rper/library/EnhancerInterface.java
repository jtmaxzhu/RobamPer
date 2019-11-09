package com.robam.rper.library;

public interface EnhancerInterface {
	
	public void setMethodInterceptor$Enhancer$(MethodInterceptor methodInterceptor);

	/**
	 *
     */
	public void setCallBacksMethod$Enhancer$(MethodInterceptor[] methodInterceptor);

	public void setTarget$Enhancer$(Object o);

	public Object getTarget$Enhancer$();

	public Object getTarget1$Enhancer$();
	/**
	 * filter
     */
	public void setCallBackFilterMethod$Enhancer$(CallbackFilter callbackFilter);

}
