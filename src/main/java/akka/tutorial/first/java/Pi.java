package akka.tutorial.first.java;

import static akka.actor.Actors.actorOf;
import scala.Option;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.dispatch.Future;
import akka.japi.Procedure;

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

		// create the master
		ActorRef master = actorOf(new UntypedActorFactory() {
			public UntypedActor create() {
				return new Master(nrOfWorkers, nrOfMessages, nrOfElements);
			}
		}).start();

		// start the calculation
		Future<Double> piResultFuture = master.sendRequestReplyFuture(new Calculate());

		piResultFuture.onComplete(new Procedure<Future<Double>>() {
			@Override
			public void apply(Future<Double> future) {
				Option<Double> resultOption = future.result();
				if (resultOption.isDefined()) {
					Double piResult = resultOption.get();
					
					System.out.println("Pi: " + piResult);
				}
			}
		});
	}
}
