package akka.tutorial.first.java;

import java.util.Arrays;

import akka.actor.ActorRef;
import akka.routing.CyclicIterator;
import akka.routing.InfiniteIterator;
import akka.routing.UntypedLoadBalancer;

public class PiRouter extends UntypedLoadBalancer {
	private final InfiniteIterator<ActorRef> workers;

	public PiRouter(ActorRef[] workers) {
		this.workers = new CyclicIterator<ActorRef>(Arrays.asList(workers));
	}

	public InfiniteIterator<ActorRef> seq() {
		return workers;
	}
}
