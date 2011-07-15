package akka.tutorial.first.java;

import static akka.actor.Actors.actorOf;
import static akka.actor.Actors.poisonPill;
import scala.Option;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.dispatch.CompletableFuture;
import akka.routing.Routing.Broadcast;

public class Master extends UntypedActor {
	private final int nrOfMessages;
	private final int nrOfElements;

	private double pi;
	private int nrOfResults;
	private long start;

	private ActorRef router;
	private Option<CompletableFuture<Object>> piResultFuture;

	public Master(int nrOfWorkers, int nrOfMessages, int nrOfElements) {
		this.nrOfMessages = nrOfMessages;
		this.nrOfElements = nrOfElements;

		// create the workers
		final ActorRef[] workers = new ActorRef[nrOfWorkers];
		for (int i = 0; i < nrOfWorkers; i++) {
			workers[i] = actorOf(Worker.class).start();
		}

		// wrap them with a load-balancing router
		router = actorOf(new UntypedActorFactory() {
			public UntypedActor create() {
				return new PiRouter(workers);
			}
		}).start();
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Calculate) {
			// Store a reference to the original caller's Future - we'll send the result back this way later.
			if (getContext().getSenderFuture().isDefined()) {
				piResultFuture = getContext().getSenderFuture();
			}
			
			// schedule work
			for (int start = 0; start < nrOfMessages; start++) {
				router.sendOneWay(new Work(start, nrOfElements), getContext());
			}

			// send a PoisonPill to all workers telling them to shut down
			// themselves
			router.sendOneWay(new Broadcast(poisonPill()));

			// send a PoisonPill to the router, telling him to shut himself down
			router.sendOneWay(poisonPill());

		} else if (message instanceof Result) {

			// handle result from the worker
			Result result = (Result) message;
			
			pi += result.getValue();
			nrOfResults += 1;
			
			// Are we done yet?
			if (nrOfResults == nrOfMessages) {
				// We're done, send the result back to the original caller
				if (piResultFuture != null) {
					piResultFuture.get().completeWithResult(pi);
				}
				
				getContext().stop();
			}
		} else
			throw new IllegalArgumentException("Unknown message [" + message
					+ "]");
	}

	@Override
	public void preStart() {
		start = System.currentTimeMillis();
	}

	@Override
	public void postStop() {
		// tell the world that the calculation is complete
		System.out.println(String.format(
				"\n\tPi estimate: \t\t%s\n\tCalculation time: \t%s millis", pi,
				(System.currentTimeMillis() - start)));
	}
}
