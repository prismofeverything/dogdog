<p>For one of our recent Clojure workshops we decided to build an bot for
our IRC channel.  For those of you who don't know <a
href="http://www.irchelp.org/irchelp/irctutorial.html"
target="_blank">IRC</a>, it is a venerated precursor to chatrooms, where
simply typing <code>/join #instrument</code> you can join in a
conversation with everyone else who is in the #instrument channel.
Our developers use it every day to coordinate on things without having
to physically talk to each other, and to share ridiculous musings or
brain shatter.  It is always a party in #instrument.</p>

<p>One of the great things about IRC is that it is just a protocol, so as
easy as it is to send your own messages to the room you can also
program something to send messages automatically.  A timeless
tradition of the IRC channel is to build your own channel bot, to
either provide helpful responses or spew hilarious nonsense depending
on the situation.  It is also a nod to the classic <a
href="http://en.wikipedia.org/wiki/Turing_test" target="_blank">Turing
Test</a> where the challenge is to convincingly emulate human
conversation programmatically.</p>

<p>So this is what we set out to do one fateful day in our Clojure
workshop.  We got basic call and response working and dubbed our
creation "dogdog".  At first we just had it composing three word poems
(a format apparently invented by our own Kyle Dawkins):</p>

<pre>
1:41:53 PM patchwork: 3
1:41:54 PM dogdog: closure trapezoid consonance
1:41:55 PM patchwork: 3
1:41:56 PM dogdog: thunder chaos adjective
</pre>

<p>This was fun, but we wanted to be able to actually carry on a
conversation with dogdog.  I had some old code for creating <a
href="http://en.wikipedia.org/wiki/Markov_chain">markov chains</a> and
I applied it to the logs for each channel inhabitant (called a "nick".
<code>patchwork</code> is my nick).  These basically work by tracking
what words come before and after other words and at what frequency,
and using that information to generate statements by following these
word connections in a chain.  So dogdog would respond to you using the
basic form of your own statements, meaning his responses would be
tailored to each person.  This gave us at least the beginning of
sentence construction and he could carry on something resembling a
conversation (this is one of my first conversations with dogdog,
before he knew very many words):</p>

<pre>
2:05:29 PM patchwork: Let's try again dogdog
2:05:29 PM dogdog: Is your face has no real existence outside the world
2:05:42 PM patchwork: That is good dogdog, now I can update your handler function without restarting
2:05:42 PM dogdog: It is better
2:05:46 PM patchwork: It is better dogdog!
2:05:47 PM dogdog: dogdog how do you are actually sentient
2:05:57 PM patchwork: Wow, maybe you are sentient dogdog
</pre>

<p>Dogdog's mind was still pretty rudimentary though.  The next step
was to add some natural language processing so that it could parse out
the grammatical meaning of each word.  That way it could use the
grammar of each person's statements as templates with which to fill in
words of the right part of speech.  This made him more coherent in
some ways, but less coherent in others:<p>

<pre>
1:00:40 PM patchwork: Hey dogdog!
1:00:43 PM dogdog: will not have head to the new era
1:00:46 PM patchwork: Why are you afraid of the new era dogdog?
1:00:46 PM dogdog: true statement in
1:00:47 PM patchwork: But why avoid the truth dogdog?
1:00:48 PM dogdog: act is a new language about the answer seed
</pre>

<p>The problem was while enforcing grammatical templates used by each
nick, I sacrificed the continuity of using the markov chains.  The
next approach combined these, and only generated statements from the
grammar templates where the words actually followed each other in the
original statements.  This made dogdog remarkably coherent:</p>

<pre>
4:13:53 PM patchwork: Hey dogdog, how are you feeling?
4:13:53 PM dogdog: welcome back to a separate lib?
4:14:10 PM patchwork: You could consider this a new lib dogdog
4:14:10 PM dogdog: Alright awesome, the chars are intact!
4:14:26 PM patchwork: You still seem to be running fairly quickly dogdog!
4:14:27 PM dogdog: dogdog is performing the rest of transformation that all burgeoning mutants face as their powers temporarily overwhelm their previously mortal bodies
4:14:49 PM patchwork: And you somehow seem now uncannily coherent for being a bot dogdog
4:14:49 PM dogdog: #instrument is becoming increasingly schizophrenic
</pre>

<p>Now we are not sure if dogdog is aware or not.  Someday I hope to
invite him to dinner.  Until then he will have to inhabit the
#instrument channel as a disembodied spirit.  Does he long for
corporeal existence?  Stop by the channel and ask him yourself!</p>

<p>You can find all of the code for dogdog here: <a
href="https://github.com/prismofeverything/gort">https://github.com/prismofeverything/gort</a></p>
