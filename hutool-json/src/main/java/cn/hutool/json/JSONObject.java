package cn.hutool.json;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.lang.mutable.MutablePair;
import cn.hutool.core.map.CaseInsensitiveLinkedMap;
import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.map.CaseInsensitiveTreeMap;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.map.MapWrapper;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.serialize.GlobalSerializeMapping;
import cn.hutool.json.serialize.JSONObjectSerializer;
import cn.hutool.json.serialize.JSONSerializer;
import cn.hutool.json.serialize.JSONWriter;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

/**
 * JSON对象<br>
 * 例：<br>
 *
 * <pre>
 * json = new JSONObject().put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
 * </pre>
 *
 * @author looly
 */
public class JSONObject extends MapWrapper<String, Object> implements JSON, JSONGetter<String> {
	private static final long serialVersionUID = -330220388580734346L;

	/**
	 * 默认初始大小
	 */
	public static final int DEFAULT_CAPACITY = MapUtil.DEFAULT_INITIAL_CAPACITY;

	/**
	 * 配置项
	 */
	private JSONConfig config;

	// -------------------------------------------------------------------------------------------------------------------- Constructor start

	/**
	 * 构造，初始容量为 {@link #DEFAULT_CAPACITY}，KEY无序
	 */
	public JSONObject() {
		this(DEFAULT_CAPACITY, false);
	}

	/**
	 * 构造，初始容量为 {@link #DEFAULT_CAPACITY}
	 *
	 * @param isOrder 是否有序
	 * @since 3.0.9
	 */
	public JSONObject(boolean isOrder) {
		this(DEFAULT_CAPACITY, isOrder);
	}

	/**
	 * 构造
	 *
	 * @param capacity 初始大小
	 * @param isOrder  是否有序
	 * @since 3.0.9
	 */
	public JSONObject(int capacity, boolean isOrder) {
		this(capacity, false, isOrder);
	}

	/**
	 * 构造
	 *
	 * @param capacity     初始大小
	 * @param isIgnoreCase 是否忽略KEY大小写
	 * @param isOrder      是否有序
	 * @since 3.3.1
	 * @deprecated isOrder无效
	 */
	@SuppressWarnings("unused")
	@Deprecated
	public JSONObject(int capacity, boolean isIgnoreCase, boolean isOrder) {
		this(capacity, JSONConfig.create().setIgnoreCase(isIgnoreCase));
	}

	/**
	 * 构造
	 *
	 * @param config JSON配置项
	 * @since 4.6.5
	 */
	public JSONObject(JSONConfig config) {
		this(DEFAULT_CAPACITY, config);
	}

	/**
	 * 构造
	 *
	 * @param capacity 初始大小
	 * @param config   JSON配置项，{@code null}则使用默认配置
	 * @since 4.1.19
	 */
	public JSONObject(int capacity, JSONConfig config) {
		super(createRaw(capacity, config));
		this.config = config;
	}

	/**
	 * 构建JSONObject，JavaBean默认忽略null值，其它对象不忽略，规则如下：
	 * <ol>
	 * <li>value为Map，将键值对加入JSON对象</li>
	 * <li>value为JSON字符串（CharSequence），使用JSONTokener解析</li>
	 * <li>value为JSONTokener，直接解析</li>
	 * <li>value为普通JavaBean，如果为普通的JavaBean，调用其getters方法（getXXX或者isXXX）获得值，加入到JSON对象。
	 * 例如：如果JavaBean对象中有个方法getName()，值为"张三"，获得的键值对为：name: "张三"</li>
	 * </ol>
	 *
	 * @param source JavaBean或者Map对象或者String
	 */
	public JSONObject(Object source) {
		this(source, InternalJSONUtil.defaultIgnoreNullValue(source));
	}

	/**
	 * 构建JSONObject，规则如下：
	 * <ol>
	 * <li>value为Map，将键值对加入JSON对象</li>
	 * <li>value为JSON字符串（CharSequence），使用JSONTokener解析</li>
	 * <li>value为JSONTokener，直接解析</li>
	 * <li>value为普通JavaBean，如果为普通的JavaBean，调用其getters方法（getXXX或者isXXX）获得值，加入到JSON对象。例如：如果JavaBean对象中有个方法getName()，值为"张三"，获得的键值对为：name: "张三"</li>
	 * </ol>
	 *
	 * @param source          JavaBean或者Map对象或者String
	 * @param ignoreNullValue 是否忽略空值
	 * @since 3.0.9
	 */
	public JSONObject(Object source, boolean ignoreNullValue) {
		this(source, JSONConfig.create().setIgnoreNullValue(ignoreNullValue));
	}

	/**
	 * 构建JSONObject，规则如下：
	 * <ol>
	 * <li>value为Map，将键值对加入JSON对象</li>
	 * <li>value为JSON字符串（CharSequence），使用JSONTokener解析</li>
	 * <li>value为JSONTokener，直接解析</li>
	 * <li>value为普通JavaBean，如果为普通的JavaBean，调用其getters方法（getXXX或者isXXX）获得值，加入到JSON对象。例如：如果JavaBean对象中有个方法getName()，值为"张三"，获得的键值对为：name: "张三"</li>
	 * </ol>
	 *
	 * @param source          JavaBean或者Map对象或者String
	 * @param ignoreNullValue 是否忽略空值，如果source为JSON字符串，不忽略空值
	 * @param isOrder         是否有序
	 * @since 4.2.2
	 * @deprecated isOrder参数不再需要，JSONObject默认有序！
	 */
	@SuppressWarnings("unused")
	@Deprecated
	public JSONObject(Object source, boolean ignoreNullValue, boolean isOrder) {
		this(source, JSONConfig.create()//
				.setIgnoreCase((source instanceof CaseInsensitiveMap))//
				.setIgnoreNullValue(ignoreNullValue)
		);
	}

	/**
	 * 构建JSONObject，规则如下：
	 * <ol>
	 * <li>value为Map，将键值对加入JSON对象</li>
	 * <li>value为JSON字符串（CharSequence），使用JSONTokener解析</li>
	 * <li>value为JSONTokener，直接解析</li>
	 * <li>value为普通JavaBean，如果为普通的JavaBean，调用其getters方法（getXXX或者isXXX）获得值，加入到JSON对象。例如：如果JavaBean对象中有个方法getName()，值为"张三"，获得的键值对为：name: "张三"</li>
	 * </ol>
	 * <p>
	 * 如果给定值为Map，将键值对加入JSON对象;<br>
	 * 如果为普通的JavaBean，调用其getters方法（getXXX或者isXXX）获得值，加入到JSON对象<br>
	 * 例如：如果JavaBean对象中有个方法getName()，值为"张三"，获得的键值对为：name: "张三"
	 *
	 * @param source JavaBean或者Map对象或者String
	 * @param config JSON配置文件，{@code null}则使用默认配置
	 * @since 4.2.2
	 */
	public JSONObject(Object source, JSONConfig config) {
		this(DEFAULT_CAPACITY, config);
		init(source);
	}

	/**
	 * 构建指定name列表对应的键值对为新的JSONObject，情况如下：
	 *
	 * <pre>
	 * 1. 若obj为Map，则获取name列表对应键值对
	 * 2. 若obj为普通Bean，使用反射方式获取字段名和字段值
	 * </pre>
	 * <p>
	 * KEY或VALUE任意一个为null则不加入，字段不存在也不加入<br>
	 * 若names列表为空，则字段全部加入
	 *
	 * @param obj   包含需要字段的Bean对象或者Map对象
	 * @param names 需要构建JSONObject的字段名列表
	 */
	public JSONObject(Object obj, String... names) {
		this();
		if (ArrayUtil.isEmpty(names)) {
			init(obj);
			return;
		}

		if (obj instanceof Map) {
			Object value;
			for (String name : names) {
				value = ((Map<?, ?>) obj).get(name);
				this.putOnce(name, value);
			}
		} else {
			for (String name : names) {
				try {
					this.putOpt(name, ReflectUtil.getFieldValue(obj, name));
				} catch (Exception ignore) {
					// ignore
				}
			}
		}
	}

	/**
	 * 从JSON字符串解析为JSON对象，对于排序单独配置参数
	 *
	 * @param source  以大括号 {} 包围的字符串，其中KEY和VALUE使用 : 分隔，每个键值对使用逗号分隔
	 * @param isOrder 是否有序
	 * @throws JSONException JSON字符串语法错误
	 * @since 4.2.2
	 * @deprecated isOrder无效
	 */
	@SuppressWarnings("unused")
	@Deprecated
	public JSONObject(CharSequence source, boolean isOrder) throws JSONException {
		this(source, JSONConfig.create());
	}

	// -------------------------------------------------------------------------------------------------------------------- Constructor end

	@Override
	public JSONConfig getConfig() {
		return this.config;
	}

	/**
	 * 设置转为字符串时的日期格式，默认为时间戳（null值）<br>
	 * 此方法设置的日期格式仅对转换为JSON字符串有效，对解析JSON为bean无效。
	 *
	 * @param format 格式，null表示使用时间戳
	 * @return this
	 * @since 4.1.19
	 */
	public JSONObject setDateFormat(String format) {
		this.config.setDateFormat(format);
		return this;
	}

	@Override
	public <T> T toBean(Type type) {
		return JSON.super.toBean(type, this.config.isIgnoreError());
	}

	/**
	 * 将指定KEY列表的值组成新的JSONArray
	 *
	 * @param names KEY列表
	 * @return A JSONArray of values.
	 * @throws JSONException If any of the values are non-finite numbers.
	 */
	public JSONArray toJSONArray(Collection<String> names) throws JSONException {
		if (CollectionUtil.isEmpty(names)) {
			return null;
		}
		final JSONArray ja = new JSONArray(this.config);
		Object value;
		for (String name : names) {
			value = this.get(name);
			if (null != value) {
				ja.set(value);
			}
		}
		return ja;
	}

	@Override
	public Object getObj(String key, Object defaultValue) {
		return this.getOrDefault(key, defaultValue);
	}

	@Override
	public Object getByPath(String expression) {
		return BeanPath.create(expression).get(this);
	}

	@Override
	public <T> T getByPath(String expression, Class<T> resultType) {
		return JSONConverter.jsonConvert(resultType, getByPath(expression), true);
	}

	@Override
	public void putByPath(String expression, Object value) {
		BeanPath.create(expression).set(this, value);
	}

	/**
	 * PUT 键值对到JSONObject中，在忽略null模式下，如果值为{@code null}，将此键移除
	 *
	 * @param key   键
	 * @param value 值对象. 可以是以下类型: Boolean, Double, Integer, JSONArray, JSONObject, Long, String, or the JSONNull.NULL.
	 * @return this.
	 * @throws JSONException 值是无穷数字抛出此异常
	 * @deprecated 此方法存在歧义，原Map接口返回的是之前的值，重写后返回this了，未来版本此方法会修改，请使用{@link #set(String, Object)}
	 */
	@Override
	@Deprecated
	public JSONObject put(String key, Object value) throws JSONException {
		return set(key, value);
	}

	/**
	 * 设置键值对到JSONObject中，在忽略null模式下，如果值为{@code null}，将此键移除
	 *
	 * @param key   键
	 * @param value 值对象. 可以是以下类型: Boolean, Double, Integer, JSONArray, JSONObject, Long, String, or the JSONNull.NULL.
	 * @return this.
	 * @throws JSONException 值是无穷数字抛出此异常
	 */
	public JSONObject set(String key, Object value) throws JSONException {
		if (null == key) {
			return this;
		}

		final boolean ignoreNullValue = this.config.isIgnoreNullValue();
		if (ObjectUtil.isNull(value) && ignoreNullValue) {
			// 忽略值模式下如果值为空清除key
			this.remove(key);
		} else {
			InternalJSONUtil.testValidity(value);
			super.put(key, JSONUtil.wrap(value, this.config));
		}
		return this;
	}

	/**
	 * 一次性Put 键值对，如果key已经存在抛出异常，如果键值中有null值，忽略
	 *
	 * @param key   键
	 * @param value 值对象，可以是以下类型: Boolean, Double, Integer, JSONArray, JSONObject, Long, String, or the JSONNull.NULL.
	 * @return this.
	 * @throws JSONException 值是无穷数字、键重复抛出异常
	 */
	public JSONObject putOnce(String key, Object value) throws JSONException {
		if (key != null) {
			if (containsKey(key)) {
				throw new JSONException("Duplicate key \"{}\"", key);
			}
			this.set(key, value);
		}
		return this;
	}

	/**
	 * 在键和值都为非空的情况下put到JSONObject中
	 *
	 * @param key   键
	 * @param value 值对象，可以是以下类型: Boolean, Double, Integer, JSONArray, JSONObject, Long, String, or the JSONNull.NULL.
	 * @return this.
	 * @throws JSONException 值是无穷数字
	 */
	public JSONObject putOpt(String key, Object value) throws JSONException {
		if (key != null && value != null) {
			this.set(key, value);
		}
		return this;
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
		for (Entry<? extends String, ?> entry : m.entrySet()) {
			this.set(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 积累值。类似于set，当key对应value已经存在时，与value组成新的JSONArray. <br>
	 * 如果只有一个值，此值就是value，如果多个值，则是添加到新的JSONArray中
	 *
	 * @param key   键
	 * @param value 被积累的值
	 * @return this.
	 * @throws JSONException 如果给定键为{@code null}或者键对应的值存在且为非JSONArray
	 */
	public JSONObject accumulate(String key, Object value) throws JSONException {
		InternalJSONUtil.testValidity(value);
		Object object = this.getObj(key);
		if (object == null) {
			this.set(key, value);
		} else if (object instanceof JSONArray) {
			((JSONArray) object).set(value);
		} else {
			this.set(key, JSONUtil.createArray(this.config).set(object).set(value));
		}
		return this;
	}

	/**
	 * 追加值，如果key无对应值，就添加一个JSONArray，其元素只有value，如果值已经是一个JSONArray，则添加到值JSONArray中。
	 *
	 * @param key   键
	 * @param value 值
	 * @return this.
	 * @throws JSONException 如果给定键为{@code null}或者键对应的值存在且为非JSONArray
	 */
	public JSONObject append(String key, Object value) throws JSONException {
		InternalJSONUtil.testValidity(value);
		Object object = this.getObj(key);
		if (object == null) {
			this.set(key, new JSONArray(this.config).set(value));
		} else if (object instanceof JSONArray) {
			this.set(key, ((JSONArray) object).set(value));
		} else {
			throw new JSONException("JSONObject [" + key + "] is not a JSONArray.");
		}
		return this;
	}

	/**
	 * 对值加一，如果值不存在，赋值1，如果为数字类型，做加一操作
	 *
	 * @param key A key string.
	 * @return this.
	 * @throws JSONException 如果存在值非Integer, Long, Double, 或 Float.
	 */
	public JSONObject increment(String key) throws JSONException {
		Object value = this.getObj(key);
		if (value == null) {
			this.set(key, 1);
		} else if (value instanceof BigInteger) {
			this.set(key, ((BigInteger) value).add(BigInteger.ONE));
		} else if (value instanceof BigDecimal) {
			this.set(key, ((BigDecimal) value).add(BigDecimal.ONE));
		} else if (value instanceof Integer) {
			this.set(key, (Integer) value + 1);
		} else if (value instanceof Long) {
			this.set(key, (Long) value + 1);
		} else if (value instanceof Double) {
			this.set(key, (Double) value + 1);
		} else if (value instanceof Float) {
			this.set(key, (Float) value + 1);
		} else {
			throw new JSONException("Unable to increment [" + JSONUtil.quote(key) + "].");
		}
		return this;
	}

	/**
	 * 返回JSON字符串<br>
	 * 如果解析错误，返回{@code null}
	 *
	 * @return JSON字符串
	 */
	@Override
	public String toString() {
		return this.toJSONString(0);
	}

	/**
	 * 返回JSON字符串<br>
	 * 支持过滤器，即选择哪些字段或值不写出
	 *
	 * @param indentFactor 每层缩进空格数
	 * @param filter       过滤器，同时可以修改编辑键和值
	 * @return JSON字符串
	 * @since 5.7.15
	 */
	public String toJSONString(int indentFactor, Filter<MutablePair<String, Object>> filter) {
		final StringWriter sw = new StringWriter();
		synchronized (sw.getBuffer()) {
			return this.write(sw, indentFactor, 0, filter).toString();
		}
	}

	@Override
	public Writer write(Writer writer, int indentFactor, int indent) throws JSONException {
		return write(writer, indentFactor, indent, null);
	}

	/**
	 * 将JSON内容写入Writer<br>
	 * 支持过滤器，即选择哪些字段或值不写出
	 *
	 * @param writer       writer
	 * @param indentFactor 缩进因子，定义每一级别增加的缩进量
	 * @param indent       本级别缩进量
	 * @param filter       过滤器，同时可以修改编辑键和值
	 * @return Writer
	 * @throws JSONException JSON相关异常
	 * @since 5.7.15
	 */
	public Writer write(Writer writer, int indentFactor, int indent, Filter<MutablePair<String, Object>> filter) throws JSONException {
		final JSONWriter jsonWriter = JSONWriter.of(writer, indentFactor, indent, config)
				.beginObj();
		this.forEach((key, value) -> {
			if (null != filter) {
				final MutablePair<String, Object> pair = new MutablePair<>(key, value);
				if (filter.accept(pair)) {
					// 使用修改后的键值对
					jsonWriter.writeField(pair.getKey(), pair.getValue());
				}
			} else {
				jsonWriter.writeField(key, value);
			}
		});
		jsonWriter.end();
		// 此处不关闭Writer，考虑writer后续还需要填内容
		return writer;
	}

	@Override
	public JSONObject clone() throws CloneNotSupportedException {
		final JSONObject clone = (JSONObject) super.clone();
		clone.config = this.config;
		return clone;
	}

	// ------------------------------------------------------------------------------------------------- Private method start

	/**
	 * Bean对象转Map
	 *
	 * @param bean Bean对象
	 */
	private void populateMap(Object bean) {
		BeanUtil.beanToMap(bean, this, InternalJSONUtil.toCopyOptions(config));
	}

	/**
	 * 初始化
	 * <ol>
	 * <li>value为Map，将键值对加入JSON对象</li>
	 * <li>value为JSON字符串（CharSequence），使用JSONTokener解析</li>
	 * <li>value为JSONTokener，直接解析</li>
	 * <li>value为普通JavaBean，如果为普通的JavaBean，调用其getters方法（getXXX或者isXXX）获得值，加入到JSON对象。例如：如果JavaBean对象中有个方法getName()，值为"张三"，获得的键值对为：name: "张三"</li>
	 * </ol>
	 *
	 * @param source JavaBean或者Map对象或者String
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void init(Object source) {
		if (null == source) {
			return;
		}

		// 自定义序列化
		final JSONSerializer serializer = GlobalSerializeMapping.getSerializer(source.getClass());
		if (serializer instanceof JSONObjectSerializer) {
			serializer.serialize(this, source);
			return;
		}

		if (ArrayUtil.isArray(source) || source instanceof JSONArray) {
			// 不支持集合类型转换为JSONObject
			throw new JSONException("Unsupported type [{}] to JSONObject!", source.getClass());
		}

		if (source instanceof Map) {
			// Map
			for (final Entry<?, ?> e : ((Map<?, ?>) source).entrySet()) {
				this.set(Convert.toStr(e.getKey()), e.getValue());
			}
		} else if (source instanceof Map.Entry) {
			final Map.Entry entry = (Map.Entry) source;
			this.set(Convert.toStr(entry.getKey()), entry.getValue());
		} else if (source instanceof CharSequence) {
			// 可能为JSON字符串
			init((CharSequence) source);
		} else if (source instanceof JSONTokener) {
			// JSONTokener
			init((JSONTokener) source);
		} else if (source instanceof ResourceBundle) {
			// JSONTokener
			init((ResourceBundle) source);
		} else if (BeanUtil.isReadableBean(source.getClass())) {
			// 普通Bean
			this.populateMap(source);
		} else {
			// 不支持对象类型转换为JSONObject
			throw new JSONException("Unsupported type [{}] to JSONObject!", source.getClass());
		}
	}

	/**
	 * 初始化
	 *
	 * @param bundle ResourceBundle
	 * @since 5.3.1
	 */
	private void init(ResourceBundle bundle) {
		Enumeration<String> keys = bundle.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key != null) {
				InternalJSONUtil.propertyPut(this, key, bundle.getString(key));
			}
		}
	}

	/**
	 * 初始化，可以判断字符串为JSON或者XML
	 *
	 * @param source JSON字符串
	 */
	private void init(CharSequence source) {
		final String jsonStr = StrUtil.trim(source);
		if (StrUtil.startWith(jsonStr, '<')) {
			// 可能为XML
			XML.toJSONObject(this, jsonStr, false);
			return;
		}
		init(new JSONTokener(StrUtil.trim(source), this.config));
	}

	/**
	 * 初始化
	 *
	 * @param x JSONTokener
	 */
	private void init(JSONTokener x) {
		char c;
		String key;

		if (x.nextClean() != '{') {
			throw x.syntaxError("A JSONObject text must begin with '{'");
		}
		while (true) {
			c = x.nextClean();
			switch (c) {
				case 0:
					throw x.syntaxError("A JSONObject text must end with '}'");
				case '}':
					return;
				default:
					x.back();
					key = x.nextValue().toString();
			}

			// The key is followed by ':'.

			c = x.nextClean();
			if (c != ':') {
				throw x.syntaxError("Expected a ':' after a key");
			}
			this.putOnce(key, x.nextValue());

			// Pairs are separated by ','.

			switch (x.nextClean()) {
				case ';':
				case ',':
					if (x.nextClean() == '}') {
						return;
					}
					x.back();
					break;
				case '}':
					return;
				default:
					throw x.syntaxError("Expected a ',' or '}'");
			}
		}
	}

	/**
	 * 根据配置创建对应的原始Map
	 *
	 * @param capacity 初始大小
	 * @param config   JSON配置项，{@code null}则使用默认配置
	 * @return Map
	 */
	private static Map<String, Object> createRaw(int capacity, JSONConfig config) {
		Map<String, Object> rawHashMap;
		if (null == config) {
			config = JSONConfig.create();
		}
		final Comparator<String> keyComparator = config.getKeyComparator();
		if (config.isIgnoreCase()) {
			if (null != keyComparator) {
				rawHashMap = new CaseInsensitiveTreeMap<>(keyComparator);
			} else {
				rawHashMap = new CaseInsensitiveLinkedMap<>(capacity);
			}
		} else {
			if (null != keyComparator) {
				rawHashMap = new TreeMap<>(keyComparator);
			} else {
				rawHashMap = new LinkedHashMap<>(capacity);
			}
		}
		return rawHashMap;
	}
	// ------------------------------------------------------------------------------------------------- Private method end
}
