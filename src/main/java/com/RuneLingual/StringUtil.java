package com.RuneLingual;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

public class StringUtil
{
	@Getter
	private int number;
	
	@Setter
	@Getter
	private String text;
	
	@Getter
	private int index;
	
	public StringUtil(int n, String t, int p)
	{
		this.number = n;
		this.text = t;
		this.index = p;
	}
	
	@Override
	public String toString()
	{
		return "(" + number + ", \"" + text + "\", " + index + ")";
	}
	
	public static StringUtil handlesNumbers(String input)
	{
		Pattern pattern = Pattern.compile("\\D*(\\d+)");
		Matcher matcher = pattern.matcher(input);
		
		int number = 0;
		String result = "";
		int index = -1;
		
		if (matcher.find())
		{
			number = Integer.parseInt(matcher.group(1));
			result = input.replaceAll("\\d+", ""); // Remove todos os conjuntos de números
			index = matcher.start(1);
		}
		
		return new StringUtil(number, result, index);
	}
}
