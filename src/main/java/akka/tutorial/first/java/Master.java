package akka.tutorial.first.java;

import static akka.actor.Actors.actorOf;
import static akka.actor.Actors.poisonPill;
import akka.actor.ActorRef;
import akka.actor.Channel;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.japi.Procedure;
import akka.routing.Routing.Broadcast;

public class Master extends UntypedActor {
	private final int nrOfMessages;
	private final int nrOfElements;

	private double pi;
	private int nrOfResults;

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
		// send a PoisonPill to all workers telling them to shut down themselves
		router.sendOneWay(new Broadcast(poisonPill()));
		// send a PoisonPill to the router, telling him to shut himself down
		router.sendOneWay(poisonPill());
	}
    
	@Override
	public void preStart() {
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
