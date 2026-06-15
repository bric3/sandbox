package sandbox

import org.gradle.api.GradleException
import org.gradle.api.provider.*
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import javax.inject.Inject

/**
 * Reads the commit hash of the current git HEAD.
 *
 * Does not handle non-git repo, repo without commit, or if git is not installed.
 */
fun ProviderFactory.gitCommit(): Provider<String> {
    return execProvider("git", "rev-list", "--no-walk=unsorted", "HEAD")
}

fun ProviderFactory.execProvider(vararg args: String): Provider<String> {
    require(args.isNotEmpty()) { "Args list is empty" }
    return this.of(ExecValueSource::class.java) { parameters.commandLine.addAll(*args) }
}

// https://docs.gradle.org/8.5/userguide/configuration_cache.html#config_cache:requirements:external_processes
internal abstract class ExecValueSource : ValueSource<String, ExecValueSource.Parameters> {
    @get:Inject abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val commandLine = parameters.commandLine.getOrElse(emptyList())
        if (commandLine.isEmpty()) {
            throw GradleException("Command line is empty")
        }
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(commandLine)
            standardOutput = output
        }
        return String(output.toByteArray(), Charset.defaultCharset()).trim { it <= ' ' }
    }

    interface Parameters : ValueSourceParameters {
        val commandLine: ListProperty<String>
    }
}