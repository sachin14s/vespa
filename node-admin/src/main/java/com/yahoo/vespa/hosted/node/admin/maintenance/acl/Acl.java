package com.yahoo.vespa.hosted.node.admin.maintenance.acl;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import com.yahoo.vespa.hosted.node.admin.ContainerAclSpec;
import com.yahoo.vespa.hosted.node.admin.maintenance.acl.iptables.Action;
import com.yahoo.vespa.hosted.node.admin.maintenance.acl.iptables.Chain;
import com.yahoo.vespa.hosted.node.admin.maintenance.acl.iptables.Command;
import com.yahoo.vespa.hosted.node.admin.maintenance.acl.iptables.FilterCommand;
import com.yahoo.vespa.hosted.node.admin.maintenance.acl.iptables.PolicyCommand;

import java.net.Inet6Address;
import java.util.List;
import java.util.Objects;

/**
 * This class represents an ACL that can be applied to a single container
 *
 * @author mpolden
 */
public class Acl {

    private final List<ContainerAclSpec> containerAclSpecs;

    public Acl(List<ContainerAclSpec> containerAclSpecs) {
        this.containerAclSpecs = ImmutableList.copyOf(containerAclSpecs);
    }

    public List<Command> toCommands() {
        final ImmutableList.Builder<Command> commands = ImmutableList.builder();
        commands.add(
                new PolicyCommand(Chain.INPUT, Action.DROP),
                new PolicyCommand(Chain.FORWARD, Action.ACCEPT),
                new PolicyCommand(Chain.OUTPUT, Action.ACCEPT),
                new FilterCommand(Chain.INPUT, Action.ACCEPT)
                        .withOption("-m", "state")
                        .withOption("--state", "RELATED,ESTABLISHED"),
                new FilterCommand(Chain.INPUT, Action.ACCEPT)
                        .withOption("-p", "ipv6-icmp"));
        containerAclSpecs.stream()
                .map(ContainerAclSpec::ipAddress)
                .filter(Acl::isIpv6)
                .map(ipAddress -> new FilterCommand(Chain.INPUT, Action.ACCEPT)
                        .withOption("-s", String.format("%s/128", ipAddress)))
                .forEach(commands::add);
        return commands.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Acl acl = (Acl) o;
        return Objects.equals(containerAclSpecs, acl.containerAclSpecs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerAclSpecs);
    }

    private static boolean isIpv6(String ipAddress) {
        return InetAddresses.forString(ipAddress) instanceof Inet6Address;
    }
}