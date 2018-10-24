package web;

import lombok.Value;

@Value
public class Message implements Comparable<Message> {

	private long id = 0;
	private String content = "";

	public Message(long id, String content) {
		this.id = id;
		this.content = content;
	}
	
	@Override
	public int compareTo(Message o) {
		return Long.compare(getId(), o.getId());
	}

	public long getId() {
		return id;
	}

	public String getContent() {
		return content;
	}
	
	
}
