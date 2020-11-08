# Updating docs with Ruby in Docker

1. Start the container interactively: `docker run -it --rm -v "$PWD/docs":/docs -p 4000:4000 ruby bash`
2. Change the working directory to docs: `cd /docs`
3. Fetch dependencies: `bundler install`
4. Run with auto reloading: `bundle exec jekyll serve -H 0.0.0.0`

