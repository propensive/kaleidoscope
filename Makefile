PROJECT=kaleidoscope/core
TESTS=kaleidoscope/test
SCALA_VERSION=2.12.6

compile: bin .bloop
	@bloop compile $(PROJECT)

watch:
	@bloop compile $(PROJECT) --watch

test:
	@bloop run $(TESTS) --watch

bin:
	@mkdir bin

clean:
	@rm -rf bin

dependencies: probation magnolia escritoire contextual

probation:
	@git clone git@github.com:propensive/probation.git --branch=fury

scala-$(SCALA_VERSION):
	@echo Downloading Scala $(SCALA_VERSION)
	@wget https://downloads.lightbend.com/scala/$(SCALA_VERSION)/scala-$(SCALA_VERSION).tgz
	@tar xf scala-2.12.6.tgz

magnolia:
	@git clone git@github.com:propensive/magnolia.git --branch=fury

escritoire:
	@git clone git@github.com:propensive/escritoire.git --branch=fury

contextual:
	@git clone git@github.com:propensive/contextual.git --branch=fury

install: scala-$(SCALA_VERSION)
	@which bloop >> /dev/null || (echo "Fetching bloop v1.0.0-M10" && curl -L https://github.com/scalacenter/bloop/releases/download/v1.0.0-M10/install.py | python)

.bloop: dependencies
	@test -n "$(JAVA_HOME)" || (echo 'JAVA_HOME has not been set' && exit 1)
	@echo Using JAVA_HOME="$(JAVA_HOME)"
	@echo Using PWD="$(PWD)"
	@mkdir -p .bloop
	@for F in $(shell ls etc); do \
		cp etc/$$F .bloop/$$F ; \
		sed -i 's/\$$JAVA_HOME/$(shell echo $(JAVA_HOME) | sed 's/\//\\\//g')/g' .bloop/$$F ; \
		sed -i 's/\$$PWD/$(shell echo $(PWD) | sed 's/\//\\\//g')/g' .bloop/$$F ; \
		echo "Written file $$F to $(PWD)/.bloop/" ; \
	done

.PHONY: compile watch test clean install dependencies
