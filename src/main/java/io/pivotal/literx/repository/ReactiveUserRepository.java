package io.pivotal.literx.repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.pivotal.literx.domain.User;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveUserRepository implements ReactiveRepository<User> {

	private final static long DEFAULT_DELAY_IN_MS = 100;

	private final long delayInMs;

	private final List<User> users;


	public ReactiveUserRepository() {
		this(DEFAULT_DELAY_IN_MS);
	}

	public ReactiveUserRepository(long delayInMs) {
		this.delayInMs = delayInMs;
		users = new ArrayList<>(Arrays.asList(User.SKYLER, User.JESSE, User.WALTER, User.SAUL));
	}

	public ReactiveUserRepository(User... users) {
		this(DEFAULT_DELAY_IN_MS, users);
	}

	/**
	 * 之所以使用delay，是为了好判断。比如某些流混合操作，一定是delay小的先出
	 */
	public ReactiveUserRepository(long delayInMs, User... users) {
		this.delayInMs = delayInMs;
		this.users = new ArrayList<>(Arrays.asList(users));
	}


	@Override
	public Mono<Void> save(Publisher<User> userPublisher) {
		return withDelay(Flux.from(userPublisher)).doOnNext(users::add).then();
	}

	@Override
	public Mono<User> findFirst() {
		return withDelay(Mono.just(users.get(0)));
	}

	@Override
	public Flux<User> findAll() {
		return withDelay(Flux.fromIterable(users));
	}

	@Override
	public Mono<User> findById(String username) {
		User user = users.stream().filter((p) -> p.getUsername().equals(username))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No user with username " + username + " found!"));
		return withDelay(Mono.just(user));
	}


	private Mono<User> withDelay(Mono<User> userMono) {
		// mono.delay只能产生一个值，0，这计数器不太能计数
		return Mono.delay(Duration.ofMillis(delayInMs))
				.flatMap(i -> userMono);
	}

	private Flux<User> withDelay(Flux<User> userFlux) {
		// flux.interval是在源源不断的产生自然数
		return Flux.interval(Duration.ofMillis(delayInMs))
				.zipWith(userFlux, (i, user) -> user);
	}

}
