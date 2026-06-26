output "vpc_id" {
  description = "ID of the project VPC."
  value       = aws_vpc.app.id
}

output "public_subnet_id" {
  description = "ID of the public subnet hosting the EC2 instances."
  value       = aws_subnet.public.id
}

output "server_elastic_ip" {
  description = "Elastic IP to use for the external DNS A record and SSH deployment."
  value       = aws_eip.app.public_ip
}

output "elastic_ip" {
  description = "Alias of server_elastic_ip for existing deployment notes."
  value       = aws_eip.app.public_ip
}

output "app_public_ip" {
  description = "Elastic public IP of the single application host, used for SSH deployment."
  value       = aws_eip.app.public_ip
}

output "app_private_ip" {
  description = "Private IP of the single application host."
  value       = aws_instance.app.private_ip
}

output "server_ssh_command" {
  description = "SSH command for the single application host."
  value       = "ssh -i <private-key-path> ubuntu@${aws_eip.app.public_ip}"
}

output "app_ssh_command" {
  description = "Alias SSH command for the single application host."
  value       = "ssh -i <private-key-path> ubuntu@${aws_eip.app.public_ip}"
}

output "ssh_command" {
  description = "SSH command for the single application host."
  value       = "ssh -i <private-key-path> ubuntu@${aws_eip.app.public_ip}"
}

output "dns_instruction" {
  description = "External DNS record to create before running the HTTPS bootstrap."
  value       = "Create an A record for your API domain pointing to ${aws_eip.app.public_ip}."
}
