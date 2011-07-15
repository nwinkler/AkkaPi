package akka.tutorial.first.java;

import static akka.actor.Actors.actorOf;
import akka.actor.ActorRef;
import akka.actor.Channel;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.japi.Procedure;

public class Master extends UntypedActor {
	private final int nrOfMessages;
	private final int nrOfElements;

	private double pi;
	private int nrOfResults;
	private long start;

	private ActorRef router;

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
	public void postStop() {
		// tell the world that the calculation is complete
		System.out.println(String.format(
				"\n\tPi estimate: \t\t%s\n\tCalculation time: \t%s millis", pi,
				(System.currentTimeMillis() - start)));
	}

	@Override
	public void preStart() {
		start = System.currentTimeMillis();
		
		become(scatter);
	}

	// message handler
	public void onReceive(Object message) {
		throw new IllegalStateException("Should be gather or scatter");
	}

	private final Procedure<Object> scatter = new Procedure<Object>() {
		public void apply(Object msg) {
			// schedule work
			for (int arg = 0; arg < nrOfMessages; arg++) {
				router.sendOneWay(new Work(arg, nrOfElements), getContext());
			}
			// Assume the gathering behavior
			become(gather(getContext().getChannel()));
		}
	};

	private Procedure<Object> gather(final Channel<Object> recipient) {
		return new Procedure<Object>() {
			public void apply(Object msg) {
				// handle result from the worker
				Result result = (Result) msg;
				
				pi += result.getValue();
				nrOfResults += 1;
				
				if (nrOfResults == nrOfMessages) {
					// send the pi result back to the guy who started the
					// calculation
					recipient.sendOneWay(pi);
					// shut ourselves down, we're done
					getContext().stop();
				}
			}
		};
	}

}
