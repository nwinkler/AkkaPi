package akka.tutorial.first.java;

import static akka.actor.Actors.actorOf;

import java.util.concurrent.CountDownLatch;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

/**
 * Hello world!
 * 
 */
public class Pi {
	public static void main(String[] args) throws Exception {
		Pi pi = new Pi();
		pi.calculate(4, 10000, 10000);
	}

	public void calculate(final int nrOfWorkers, final int nrOfElements,
			final int nrOfMessages) throws Exception {

		// this latch is only plumbing to know when the calculation is completed
		final CountDownLatch latch = new CountDownLatch(1);

		// create the master
		ActorRef master = actorOf(new UntypedActorFactory() {
			public UntypedActor create() {
				return new Master(nrOfWorkers, nrOfMessages, nrOfElements,
						latch);
			}
		}).start();

		// start the calculation
		master.sendOneWay(new Calculate());

		// wait for master to shut down
		latch.await();
	}
}
